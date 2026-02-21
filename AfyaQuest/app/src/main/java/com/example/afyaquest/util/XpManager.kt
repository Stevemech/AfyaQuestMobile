package com.example.afyaquest.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * XP Manager - Centralized XP management system
 * Ported from TypeScript xpManager.ts
 */

// Extension to create DataStore instance
private val Context.xpDataStore: DataStore<Preferences> by preferencesDataStore(name = "xp_data")

data class XpData(
    val totalXP: Int = 0,
    val dailyXP: Int = 0,
    val weeklyXP: Int = 0,
    val streak: Int = 0,
    val level: Int = 0,
    val lives: Int = 10,
    val lastResetDate: String = "",
    val rank: String = "Beginner"
)

data class XpTransaction(
    val amount: Int,
    val reason: String,
    val timestamp: String,
    val type: TransactionType
)

enum class TransactionType {
    EARN, SPEND
}

object XpRewards {
    const val EASY_QUESTION = 10
    const val MEDIUM_QUESTION = 20
    const val HARD_QUESTION = 30
    const val DAILY_QUESTION_BONUS = 25 // For completing all daily questions
    const val CHECK_IN = 10
    const val COMPLETE_VISIT = 50
    const val DAILY_REPORT = 25
    const val STREAK_BONUS = 10 // Flat bonus per streak day
    const val VIDEO_WATCHED = 15
    const val MODULE_COMPLETED = 50
    const val MAX_LIVES = 10
}

@Singleton
class XpManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.xpDataStore

    private object PreferencesKeys {
        val TOTAL_XP = intPreferencesKey("total_xp")
        val DAILY_XP = intPreferencesKey("daily_xp")
        val WEEKLY_XP = intPreferencesKey("weekly_xp")
        val STREAK = intPreferencesKey("streak")
        val LEVEL = intPreferencesKey("level")
        val LIVES = intPreferencesKey("lives")
        val LAST_RESET_DATE = stringPreferencesKey("last_reset_date")
        val RANK = stringPreferencesKey("rank")
    }

    companion object {
        /** Progressive level thresholds */
        private val LEVEL_THRESHOLDS = listOf(
            0,     // Level 0
            100,   // Level 1
            250,   // Level 2
            500,   // Level 3
            850,   // Level 4
            1300,  // Level 5
            1900,  // Level 6
            2650,  // Level 7
            3550,  // Level 8
            4600   // Level 9
        )

        /** XP per level beyond the threshold table (level 10+) */
        private const val XP_PER_LEVEL_AFTER_TABLE = 1200

        private fun getCurrentDateString(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return dateFormat.format(Date())
        }
    }

    /**
     * Get current XP data as Flow
     */
    fun getXpDataFlow(): Flow<XpData> = dataStore.data.map { preferences ->
        val today = getCurrentDateString()
        val lastResetDate = preferences[PreferencesKeys.LAST_RESET_DATE] ?: today

        // Reset daily XP if date changed
        val dailyXP = if (lastResetDate != today) {
            0
        } else {
            preferences[PreferencesKeys.DAILY_XP] ?: 0
        }

        val totalXP = preferences[PreferencesKeys.TOTAL_XP] ?: 0
        val level = preferences[PreferencesKeys.LEVEL] ?: calculateLevel(totalXP)
        val lives = preferences[PreferencesKeys.LIVES] ?: 10

        XpData(
            totalXP = totalXP,
            dailyXP = dailyXP,
            weeklyXP = preferences[PreferencesKeys.WEEKLY_XP] ?: 0,
            streak = preferences[PreferencesKeys.STREAK] ?: 0,
            level = level,
            lives = lives,
            lastResetDate = lastResetDate,
            rank = calculateRank(level)
        )
    }

    /**
     * Get current XP data (suspend function for one-time read)
     */
    suspend fun getXpData(): XpData {
        return getXpDataFlow().first()
    }

    /**
     * Add XP points
     */
    suspend fun addXP(amount: Int, reason: String): XpData {
        val currentData = getXpData()
        val newTotalXP = currentData.totalXP + amount
        val newDailyXP = currentData.dailyXP + amount
        val newWeeklyXP = currentData.weeklyXP + amount
        val newLevel = calculateLevel(newTotalXP)

        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_XP] = newTotalXP
            preferences[PreferencesKeys.DAILY_XP] = newDailyXP
            preferences[PreferencesKeys.WEEKLY_XP] = newWeeklyXP
            preferences[PreferencesKeys.LEVEL] = newLevel
            preferences[PreferencesKeys.RANK] = calculateRank(newLevel)
        }

        return getXpData()
    }

    /**
     * Spend XP points (for future features)
     */
    suspend fun spendXP(amount: Int, reason: String): Boolean {
        val currentData = getXpData()

        if (currentData.totalXP < amount) {
            return false // Not enough XP
        }

        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_XP] = currentData.totalXP - amount
        }

        return true
    }

    /**
     * Update streak
     */
    suspend fun updateStreak(increment: Boolean = true): Int {
        val currentData = getXpData()

        val newStreak = if (increment) {
            val incrementedStreak = currentData.streak + 1
            // Add flat streak bonus XP
            addXP(XpRewards.STREAK_BONUS, "$incrementedStreak day streak bonus!")
            incrementedStreak
        } else {
            0
        }

        dataStore.edit { preferences ->
            preferences[PreferencesKeys.STREAK] = newStreak
        }

        return newStreak
    }

    /**
     * Get current lives
     */
    suspend fun getLives(): Int {
        val data = getXpData()
        return data.lives
    }

    /**
     * Add lives (capped at MAX_LIVES)
     */
    suspend fun addLives(amount: Int, reason: String = "Earned lives"): Int {
        val currentData = getXpData()
        val newLives = minOf(currentData.lives + amount, XpRewards.MAX_LIVES)

        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LIVES] = newLives
        }

        return newLives
    }

    /**
     * Remove lives
     */
    suspend fun removeLives(amount: Int, reason: String = "Lost lives"): Int {
        val currentData = getXpData()
        val newLives = maxOf(currentData.lives - amount, 0)

        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LIVES] = newLives
        }

        return newLives
    }

    /**
     * Reset lives to starting value
     */
    suspend fun resetLives() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LIVES] = XpRewards.MAX_LIVES
        }
    }

    /**
     * Initialize lives if they're at 0 or undefined
     */
    suspend fun initializeLivesIfNeeded() {
        val lives = getLives()
        if (lives == 0) {
            resetLives()
        }
    }

    /**
     * Overwrite local XP data with values from the server.
     * Called on dashboard load so the app reflects the latest server state.
     */
    suspend fun syncFromServer(totalXP: Int, streak: Int, level: Int, rank: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_XP] = totalXP
            preferences[PreferencesKeys.STREAK] = streak
            preferences[PreferencesKeys.LEVEL] = level
            preferences[PreferencesKeys.RANK] = rank
        }
    }

    /**
     * Reset daily XP (called at midnight or on app load)
     */
    suspend fun resetDailyXP() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_XP] = 0
            preferences[PreferencesKeys.LAST_RESET_DATE] = getCurrentDateString()
        }
    }

    /**
     * Get XP needed for next level
     */
    fun getXPForNextLevel(currentXP: Int, currentLevel: Int): Int {
        val nextLevelXP = getXPThresholdForLevel(currentLevel + 1)
        return nextLevelXP - currentXP
    }

    /**
     * Get level progress percentage
     */
    fun getLevelProgress(currentXP: Int, currentLevel: Int): Float {
        val currentLevelXP = getXPThresholdForLevel(currentLevel)
        val nextLevelXP = getXPThresholdForLevel(currentLevel + 1)
        val progressXP = currentXP - currentLevelXP
        val neededXP = nextLevelXP - currentLevelXP
        if (neededXP <= 0) return 100f
        return (progressXP.toFloat() / neededXP.toFloat()) * 100f
    }

    /**
     * Get the XP threshold for a given level
     */
    private fun getXPThresholdForLevel(level: Int): Int {
        return if (level < LEVEL_THRESHOLDS.size) {
            LEVEL_THRESHOLDS[level]
        } else {
            // Level 10+: last threshold + 1200 per additional level
            val lastThreshold = LEVEL_THRESHOLDS.last()
            val extraLevels = level - (LEVEL_THRESHOLDS.size - 1)
            lastThreshold + extraLevels * XP_PER_LEVEL_AFTER_TABLE
        }
    }

    /**
     * Calculate level from total XP using progressive thresholds
     */
    private fun calculateLevel(totalXP: Int): Int {
        // Check against threshold table
        for (i in LEVEL_THRESHOLDS.indices.reversed()) {
            if (totalXP >= LEVEL_THRESHOLDS[i]) {
                // Check if beyond the table
                if (i == LEVEL_THRESHOLDS.size - 1 && totalXP > LEVEL_THRESHOLDS[i]) {
                    val extraXP = totalXP - LEVEL_THRESHOLDS[i]
                    return i + (extraXP / XP_PER_LEVEL_AFTER_TABLE)
                }
                return i
            }
        }
        return 0
    }

    /**
     * Calculate rank from level
     */
    private fun calculateRank(level: Int): String {
        return when {
            level < 2 -> "Beginner"
            level < 5 -> "Novice"
            level < 10 -> "Apprentice"
            level < 15 -> "Practitioner"
            level < 20 -> "Expert"
            level < 30 -> "Master"
            else -> "Grand Master"
        }
    }
}
