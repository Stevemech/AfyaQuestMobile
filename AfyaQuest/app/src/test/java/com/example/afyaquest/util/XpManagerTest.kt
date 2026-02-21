package com.example.afyaquest.util

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
}
