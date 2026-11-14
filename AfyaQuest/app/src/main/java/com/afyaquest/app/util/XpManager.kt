package com.afyaquest.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.time.LocalDate
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
    val rank: String = "Beginner",
    /** Last day (yyyy-MM-dd) on which XP was earned; drives the streak. Empty if never. */
    val lastStreakDate: String = "",
    /** Best streak ever reached. Unlike [streak] it never drops, so achievements can rely on it. */
    val longestStreak: Int = 0
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
        val LAST_STREAK_DATE = stringPreferencesKey("last_streak_date")
        val LONGEST_STREAK = intPreferencesKey("longest_streak")
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

        /**
         * The streak after earning XP on [today], given the stored streak and the last day XP was
         * earned: unchanged for another activity on the same day, +1 when the last activity was
         * yesterday, and back to 1 after a gap (or for the very first activity).
         */
        fun streakAfterActivity(storedStreak: Int, lastStreakDate: String, today: String): Int = when {
            lastStreakDate == today -> storedStreak.coerceAtLeast(1)
            lastStreakDate == dayBefore(today) -> storedStreak.coerceAtLeast(0) + 1
            else -> 1
        }

        /**
         * The streak to show on [today]. A streak is alive if XP was earned today or yesterday
         * (there is still time to extend it); after a missed day it reads 0.
         */
        fun visibleStreak(storedStreak: Int, lastStreakDate: String, today: String): Int =
            if (lastStreakDate == today || lastStreakDate == dayBefore(today)) storedStreak else 0

        private fun dayBefore(isoDate: String): String? = try {
            LocalDate.parse(isoDate).minusDays(1).toString()
        } catch (e: Exception) {
            null
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

        val lastStreakDate = preferences[PreferencesKeys.LAST_STREAK_DATE] ?: ""
        XpData(
            totalXP = totalXP,
            dailyXP = dailyXP,
            weeklyXP = preferences[PreferencesKeys.WEEKLY_XP] ?: 0,
            streak = visibleStreak(preferences[PreferencesKeys.STREAK] ?: 0, lastStreakDate, today),
            level = level,
            lives = lives,
            lastResetDate = lastResetDate,
            rank = calculateRank(level),
            lastStreakDate = lastStreakDate,
            longestStreak = preferences[PreferencesKeys.LONGEST_STREAK] ?: 0
        )
    }

    /**
     * Get current XP data (suspend function for one-time read)
     */
    suspend fun getXpData(): XpData {
        return getXpDataFlow().first()
    }

    /**
     * Add XP points. Earning XP is also what keeps the daily streak going, so the streak is
     * updated in the same write.
     */
    suspend fun addXP(amount: Int, reason: String): XpData {
        val today = getCurrentDateString()
        dataStore.edit { preferences ->
            val lastResetDate = preferences[PreferencesKeys.LAST_RESET_DATE] ?: today
            val dailySoFar = if (lastResetDate != today) 0 else preferences[PreferencesKeys.DAILY_XP] ?: 0
            val newTotalXP = (preferences[PreferencesKeys.TOTAL_XP] ?: 0) + amount
            val newLevel = calculateLevel(newTotalXP)

            preferences[PreferencesKeys.TOTAL_XP] = newTotalXP
            preferences[PreferencesKeys.DAILY_XP] = dailySoFar + amount
            preferences[PreferencesKeys.LAST_RESET_DATE] = today
            preferences[PreferencesKeys.WEEKLY_XP] = (preferences[PreferencesKeys.WEEKLY_XP] ?: 0) + amount
            preferences[PreferencesKeys.LEVEL] = newLevel
            preferences[PreferencesKeys.RANK] = calculateRank(newLevel)

            if (amount > 0) {
                val newStreak = streakAfterActivity(
                    storedStreak = preferences[PreferencesKeys.STREAK] ?: 0,
                    lastStreakDate = preferences[PreferencesKeys.LAST_STREAK_DATE] ?: "",
                    today = today
                )
                preferences[PreferencesKeys.STREAK] = newStreak
                preferences[PreferencesKeys.LAST_STREAK_DATE] = today
                preferences[PreferencesKeys.LONGEST_STREAK] =
                    maxOf(preferences[PreferencesKeys.LONGEST_STREAK] ?: 0, newStreak)
            }
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
     * What the account sync uploads: the raw stored values (the streak here is NOT faded to 0 after
     * a missed day; the server keeps the pair of streak and last-activity day).
     */
    data class SyncState(
        val totalXP: Int = 0,
        val lives: Int = XpRewards.MAX_LIVES,
        val storedStreak: Int = 0,
        val lastStreakDate: String = "",
        val longestStreak: Int = 0
    )

    fun syncStateFlow(): Flow<SyncState> = dataStore.data.map { preferences ->
        SyncState(
            totalXP = preferences[PreferencesKeys.TOTAL_XP] ?: 0,
            lives = preferences[PreferencesKeys.LIVES] ?: XpRewards.MAX_LIVES,
            storedStreak = preferences[PreferencesKeys.STREAK] ?: 0,
            lastStreakDate = preferences[PreferencesKeys.LAST_STREAK_DATE] ?: "",
            longestStreak = preferences[PreferencesKeys.LONGEST_STREAK] ?: 0
        )
    }

    suspend fun getSyncState(): SyncState = syncStateFlow().first()

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
     * Merge the account state held by the server into this device.
     *
     * XP never goes down (the device may hold XP earned offline that the server has not seen yet),
     * and the streak entry with the later activity day wins. With [adoptLives] the server's lives
     * replace the local ones; that is only right when this device has no history for the account
     * (fresh install or a different user signed in), otherwise the local count is the newer one.
     */
    suspend fun mergeFromServer(
        totalXP: Int,
        lives: Int?,
        streak: Int,
        lastStreakDate: String,
        adoptLives: Boolean,
        longestStreak: Int = 0
    ) {
        dataStore.edit { preferences ->
            val mergedXP = maxOf(preferences[PreferencesKeys.TOTAL_XP] ?: 0, totalXP)
            val mergedLevel = calculateLevel(mergedXP)
            preferences[PreferencesKeys.TOTAL_XP] = mergedXP
            preferences[PreferencesKeys.LEVEL] = mergedLevel
            preferences[PreferencesKeys.RANK] = calculateRank(mergedLevel)

            val localDate = preferences[PreferencesKeys.LAST_STREAK_DATE] ?: ""
            val localStreak = preferences[PreferencesKeys.STREAK] ?: 0
            if (lastStreakDate > localDate) {
                preferences[PreferencesKeys.STREAK] = streak
                preferences[PreferencesKeys.LAST_STREAK_DATE] = lastStreakDate
            } else if (lastStreakDate == localDate && streak > localStreak) {
                preferences[PreferencesKeys.STREAK] = streak
            }
            preferences[PreferencesKeys.LONGEST_STREAK] = maxOf(
                preferences[PreferencesKeys.LONGEST_STREAK] ?: 0,
                longestStreak,
                preferences[PreferencesKeys.STREAK] ?: 0
            )

            if (adoptLives && lives != null) {
                preferences[PreferencesKeys.LIVES] = lives.coerceIn(0, XpRewards.MAX_LIVES)
            }
        }
    }

    /** Forget everything (used when a different account signs in on this device). */
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
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
