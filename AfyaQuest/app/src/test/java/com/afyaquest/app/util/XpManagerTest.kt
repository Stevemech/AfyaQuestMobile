package com.afyaquest.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for XpManager
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class XpManagerTest {

    private lateinit var context: Context
    private lateinit var xpManager: XpManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        xpManager = XpManager(context)
    }

    @Test
    fun `getXPForNextLevel returns correct XP needed`() = runBlocking {
        // Level 0 -> 1 requires 100 XP
        assertEquals(100, xpManager.getXPForNextLevel(0, 0))
        // At 50 XP, level 0, need 50 more for level 1
        assertEquals(50, xpManager.getXPForNextLevel(50, 0))
        // Level 1 -> 2 requires 250 XP total, so from 100 XP need 150
        assertEquals(150, xpManager.getXPForNextLevel(100, 1))
    }

    @Test
    fun `getLevelProgress returns correct progress percentage`() = runBlocking {
        // Level 0: 0-100 XP range. At 0 XP = 0%
        assertEquals(0f, xpManager.getLevelProgress(0, 0), 0.01f)
        // Level 0: 0-100 XP range. At 50 XP = 50%
        assertEquals(50f, xpManager.getLevelProgress(50, 0), 0.01f)
        // Level 1: 100-250 XP range. At 100 XP = 0%
        assertEquals(0f, xpManager.getLevelProgress(100, 1), 0.01f)
        // Level 1: 100-250 XP range. At 175 XP = 50%
        assertEquals(50f, xpManager.getLevelProgress(175, 1), 0.01f)
    }

    @Test
    fun `addXP increases total XP correctly`() = runBlocking {
        val initialData = xpManager.getXpDataFlow().first()
        val initialXP = initialData.totalXP

        val result = xpManager.addXP(100, "Test XP")

        assertEquals(initialXP + 100, result.totalXP)
    }

    @Test
    fun `removeLives decreases lives correctly`() = runBlocking {
        xpManager.initializeLivesIfNeeded()

        val result = xpManager.removeLives(1, "Test penalty")

        assertTrue(result < 10)
    }

    @Test
    fun `addLives caps at MAX_LIVES`() = runBlocking {
        xpManager.resetLives() // Start at 10

        val result = xpManager.addLives(5, "Test reward")

        assertEquals(XpRewards.MAX_LIVES, result) // Should cap at 10
    }

    @Test
    fun `XpRewards constants have correct values`() {
        assertEquals(10, XpRewards.EASY_QUESTION)
        assertEquals(20, XpRewards.MEDIUM_QUESTION)
        assertEquals(30, XpRewards.HARD_QUESTION)
        assertEquals(25, XpRewards.DAILY_QUESTION_BONUS)
        assertEquals(10, XpRewards.CHECK_IN)
        assertEquals(50, XpRewards.COMPLETE_VISIT)
        assertEquals(25, XpRewards.DAILY_REPORT)
        assertEquals(10, XpRewards.STREAK_BONUS)
        assertEquals(15, XpRewards.VIDEO_WATCHED)
        assertEquals(50, XpRewards.MODULE_COMPLETED)
        assertEquals(10, XpRewards.MAX_LIVES)
    }

    @Test
    fun `XpData defaults are clean for new accounts`() {
        val defaults = XpData()
        assertEquals(0, defaults.totalXP)
        assertEquals(0, defaults.streak)
        assertEquals(0, defaults.level)
        assertEquals(10, defaults.lives)
        assertEquals("Beginner", defaults.rank)
    }

    // ---- streak ----

    @Test
    fun `streak rules`() {
        // First ever activity starts a streak of 1.
        assertEquals(1, XpManager.streakAfterActivity(0, "", "2026-11-12"))
        // Another activity on the same day does not add a day.
        assertEquals(3, XpManager.streakAfterActivity(3, "2026-11-12", "2026-11-12"))
        // Activity the day after extends it, also across a month boundary.
        assertEquals(4, XpManager.streakAfterActivity(3, "2026-11-11", "2026-11-12"))
        assertEquals(8, XpManager.streakAfterActivity(7, "2026-10-31", "2026-11-01"))
        // A missed day starts again from 1.
        assertEquals(1, XpManager.streakAfterActivity(9, "2026-11-10", "2026-11-12"))
    }

    @Test
    fun `a streak is shown while it can still be extended and reads 0 after a missed day`() {
        assertEquals(5, XpManager.visibleStreak(5, "2026-11-12", "2026-11-12"))
        assertEquals(5, XpManager.visibleStreak(5, "2026-11-11", "2026-11-12"))
        assertEquals(0, XpManager.visibleStreak(5, "2026-11-10", "2026-11-12"))
        assertEquals(0, XpManager.visibleStreak(0, "", "2026-11-12"))
    }

    @Test
    fun `earning XP starts today's streak once and remembers the best streak`() = runBlocking {
        xpManager.clearAll()

        val first = xpManager.addXP(20, "daily question")
        val second = xpManager.addXP(50, "quiz, same day")

        assertEquals(70, second.totalXP)
        assertEquals(70, second.dailyXP)
        assertEquals(1, first.streak)
        assertEquals(1, second.streak)
        assertEquals(1, second.longestStreak)
        assertEquals(second.lastResetDate, second.lastStreakDate)
    }

    // ---- merging the account state from the server ----

    @Test
    fun `server state never lowers XP and the later streak day wins`() = runBlocking {
        xpManager.clearAll()
        xpManager.addXP(200, "earned offline")
        val today = xpManager.getSyncState().lastStreakDate

        // The server is behind on XP and has an older streak entry.
        xpManager.mergeFromServer(totalXP = 150, lives = 3, streak = 6, lastStreakDate = "2020-01-01", adoptLives = false, longestStreak = 6)

        val state = xpManager.getSyncState()
        assertEquals(200, state.totalXP)
        assertEquals(1, state.storedStreak)
        assertEquals(today, state.lastStreakDate)
        assertEquals(10, state.lives)          // local lives are the newer ones
        assertEquals(6, state.longestStreak)   // the best streak ever is kept
    }

    @Test
    fun `a device with no history takes the server's totals, level and lives`() = runBlocking {
        xpManager.clearAll()

        xpManager.mergeFromServer(totalXP = 520, lives = 4, streak = 3, lastStreakDate = "2026-11-12", adoptLives = true, longestStreak = 3)

        val data = xpManager.getXpData()
        assertEquals(520, data.totalXP)
        assertEquals(3, data.level)
        assertEquals(4, data.lives)
        assertEquals(3, xpManager.getSyncState().storedStreak)
    }
}
