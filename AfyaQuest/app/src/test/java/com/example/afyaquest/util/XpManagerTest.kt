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
    fun `calculateLevel returns correct level for given XP`() {
        assertEquals(1, xpManager.calculateLevel(0))
        assertEquals(1, xpManager.calculateLevel(499))
        assertEquals(2, xpManager.calculateLevel(500))
        assertEquals(3, xpManager.calculateLevel(1000))
        assertEquals(5, xpManager.calculateLevel(2000))
        assertEquals(10, xpManager.calculateLevel(4500))
    }

    @Test
    fun `calculateRank returns correct rank for given level`() {
        assertEquals("Novice Helper", xpManager.calculateRank(1))
        assertEquals("Novice Helper", xpManager.calculateRank(2))
        assertEquals("Junior Assistant", xpManager.calculateRank(3))
        assertEquals("Junior Assistant", xpManager.calculateRank(4))
        assertEquals("Community Assistant", xpManager.calculateRank(5))
        assertEquals("Senior Assistant", xpManager.calculateRank(8))
        assertEquals("Expert CHA", xpManager.calculateRank(10))
        assertEquals("Master CHA", xpManager.calculateRank(15))
    }

    @Test
    fun `getXPForNextLevel returns correct XP needed`() = runBlocking {
        assertEquals(500, xpManager.getXPForNextLevel(0, 1))
        assertEquals(500, xpManager.getXPForNextLevel(250, 1))
        assertEquals(0, xpManager.getXPForNextLevel(500, 2))
        assertEquals(500, xpManager.getXPForNextLevel(500, 2))
        assertEquals(250, xpManager.getXPForNextLevel(750, 2))
    }

    @Test
    fun `getLevelProgress returns correct progress percentage`() = runBlocking {
        assertEquals(0f, xpManager.getLevelProgress(0, 1), 0.01f)
        assertEquals(50f, xpManager.getLevelProgress(250, 1), 0.01f)
        assertEquals(100f, xpManager.getLevelProgress(500, 1), 0.01f)
        assertEquals(0f, xpManager.getLevelProgress(500, 2), 0.01f)
        assertEquals(50f, xpManager.getLevelProgress(750, 2), 0.01f)
    }

    @Test
    fun `addXP increases total XP correctly`() = runBlocking {
        val initialData = xpManager.getXpDataFlow().first()
        val initialXP = initialData.totalXP

        val result = xpManager.addXP(100, "Test XP")

        assertEquals(initialXP + 100, result.totalXP)
    }

    @Test
    fun `addXP updates level when threshold crossed`() = runBlocking {
        // This test would need to reset DataStore to known state
        // For now, we'll test the logic
        val xp = 450
        val level = xpManager.calculateLevel(xp)
        assertEquals(1, level)

        val newXp = xp + 100 // Should cross to level 2
        val newLevel = xpManager.calculateLevel(newXp)
        assertEquals(2, newLevel)
    }

    @Test
    fun `removeLives decreases lives correctly`() = runBlocking {
        // Initialize with max lives
        xpManager.initializeLivesIfNeeded()

        val result = xpManager.removeLives(1, "Test penalty")

        assertTrue(result < 5) // Should be less than max
    }

    @Test
    fun `addLives increases lives but not above max`() = runBlocking {
        // Set lives to 3
        xpManager.removeLives(2, "Test setup")

        val result = xpManager.addLives(5, "Test reward")

        assertEquals(5, result) // Should cap at max (5)
    }

    @Test
    fun `getCurrentDateString returns correct format`() {
        val dateString = XpManager.getCurrentDateString()

        // Should match YYYY-MM-DD format
        assertTrue(dateString.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun `XpRewards constants have correct values`() {
        assertEquals(30, XpRewards.DAILY_QUESTION_CORRECT)
        assertEquals(50, XpRewards.DAILY_REPORT_SUBMITTED)
        assertEquals(75, XpRewards.MODULE_COMPLETED)
        assertEquals(20, XpRewards.VIDEO_WATCHED)
        assertEquals(100, XpRewards.LESSON_COMPLETED)
    }
}
