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
    val totalXP: Int = 1432, // Starting XP
    val dailyXP: Int = 0,
    val weeklyXP: Int = 0,
    val streak: Int = 3, // Starting streak
    val level: Int = 5, // Starting level
    val lives: Int = 10, // Starting lives
    val lastResetDate: String = "",
    val rank: String = "Novice"
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
    const val CHECK_IN = 50
    const val COMPLETE_VISIT = 100
    const val DAILY_QUESTION_CORRECT = 30
    const val DAILY_QUESTION_BONUS = 50 // For completing all daily questions
    const val DAILY_REPORT = 50
    const val STREAK_BONUS = 25 // Per day of streak
    const val VIDEO_WATCHED = 20
    const val MODULE_COMPLETED = 75
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

    /**
     * Get current XP data as Flow
     */
    fun getXpDataFlow(): Flow<XpData> = dataStore.data.map { preferences ->
        val today = XpManager.getCurrentDateString()
        val lastResetDate = preferences[PreferencesKeys.LAST_RESET_DATE] ?: today

        // Reset daily XP if date changed
        val dailyXP = if (lastResetDate != today) {
            0
        } else {
            preferences[PreferencesKeys.DAILY_XP] ?: 0
        }

        val totalXP = preferences[PreferencesKeys.TOTAL_XP] ?: 1432
        val level = preferences[PreferencesKeys.LEVEL] ?: calculateLevel(totalXP)
        val lives = preferences[PreferencesKeys.LIVES] ?: 10

        XpData(
            totalXP = totalXP,
            dailyXP = dailyXP,
            weeklyXP = preferences[PreferencesKeys.WEEKLY_XP] ?: 0,
            streak = preferences[PreferencesKeys.STREAK] ?: 3,
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
            // Add streak bonus XP
            addXP(XpRewards.STREAK_BONUS * incrementedStreak, "$incrementedStreak day streak bonus!")
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
     * Add lives
     */
    suspend fun addLives(amount: Int, reason: String = "Earned lives"): Int {
        val currentData = getXpData()
        val newLives = currentData.lives + amount // No upper limit

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
            preferences[PreferencesKeys.LIVES] = 10
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
     * Reset daily XP (called at midnight or on app load)
     */
    suspend fun resetDailyXP() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_XP] = 0
            preferences[PreferencesKeys.LAST_RESET_DATE] = XpManager.getCurrentDateString()
        }
    }

    /**
     * Get XP needed for next level
     */
    fun getXPForNextLevel(currentXP: Int, currentLevel: Int): Int {
        val currentLevelXP = (currentLevel - 1) * 500
        val nextLevelXP = currentLevel * 500
        return nextLevelXP - currentXP
    }

    /**
     * Get level progress percentage
     */
    fun getLevelProgress(currentXP: Int, currentLevel: Int): Float {
        val currentLevelXP = (currentLevel - 1) * 500
        val nextLevelXP = currentLevel * 500
        val progressXP = currentXP - currentLevelXP
        val neededXP = nextLevelXP - currentLevelXP
        return (progressXP.toFloat() / neededXP.toFloat()) * 100f
    }

    /**
     * Calculate level from total XP
     * Every 500 XP = 1 level
     */
    private fun calculateLevel(totalXP: Int): Int {
        return (totalXP / 500) + 1
    }

    /**
     * Calculate rank from level
     */
    private fun calculateRank(level: Int): String {
        return when {
            level < 5 -> "Novice"
            level < 10 -> "Apprentice"
            level < 20 -> "Practitioner"
            level < 30 -> "Expert"
            level < 40 -> "Master"
            else -> "Grand Master"
        }
    }

    companion object {
        private fun getCurrentDateString(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return dateFormat.format(Date())
        }
    }
}
