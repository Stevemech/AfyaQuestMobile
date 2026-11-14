package com.afyaquest.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(name = "learning_progress")

/** Result of the most recent daily-questions session. */
data class DailyQuestionsResult(
    val date: String,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val xpEarned: Int
)

/**
 * Everything in this store that belongs to the account rather than the device: it is uploaded
 * with the account's state and restored on another phone. (Itinerary stops stay out of it: they
 * are synced one by one as visits.)
 */
data class LearningProgressSnapshot(
    val watchedVideos: Set<String> = emptySet(),
    val completedQuizzes: Set<String> = emptySet(),
    val completedLessons: Set<String> = emptySet(),
    /** Days (yyyy-MM-dd) on which a daily report was submitted. */
    val reportDates: Set<String> = emptySet(),
    val dailyQuestions: DailyQuestionsResult? = null
)

/** One answered daily question: which answer was picked, whether it was right and what it paid. */
data class DailyAnswer(
    val questionId: String,
    val selectedIndex: Int,
    val correct: Boolean,
    val xp: Int
) {
    fun encode(): String = listOf(selectedIndex, if (correct) 1 else 0, xp, questionId).joinToString("|")

    companion object {
        /** The id comes last so that it may itself contain the separator. */
        fun decode(raw: String): DailyAnswer? {
            val parts = raw.split("|", limit = 4)
            if (parts.size != 4) return null
            return DailyAnswer(
                questionId = parts[3],
                selectedIndex = parts[0].toIntOrNull() ?: return null,
                correct = parts[1] == "1",
                xp = parts[2].toIntOrNull() ?: return null
            )
        }
    }
}

/** Visited / total stops for a given day's itinerary. */
data class ItineraryProgress(
    val date: String,
    val visited: Int,
    val total: Int
) {
    val isComplete: Boolean get() = total > 0 && visited >= total
}

@Singleton
class ProgressDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val WATCHED_VIDEOS = stringSetPreferencesKey("watched_videos")
        private val COMPLETED_QUIZZES = stringSetPreferencesKey("completed_quizzes")
        private val COMPLETED_LESSONS = stringSetPreferencesKey("completed_lessons")
        private val COMPLETED_STOPS = stringSetPreferencesKey("completed_stops")

        // Daily questions: one session per day
        private val DAILY_QUESTIONS_DATE = stringPreferencesKey("daily_questions_date")
        private val DAILY_QUESTIONS_CORRECT = intPreferencesKey("daily_questions_correct")
        private val DAILY_QUESTIONS_TOTAL = intPreferencesKey("daily_questions_total")
        private val DAILY_QUESTIONS_XP = intPreferencesKey("daily_questions_xp")

        // Daily questions answered so far in a session that is not finished yet
        private val DAILY_SESSION_DATE = stringPreferencesKey("daily_session_date")
        private val DAILY_SESSION_ANSWERS = stringSetPreferencesKey("daily_session_answers")

        // Itinerary: how many stops today's route has (so the hub can show N of M)
        private val ITINERARY_DATE = stringPreferencesKey("itinerary_date")
        private val ITINERARY_TOTAL = intPreferencesKey("itinerary_total")

        // Today's route as last downloaded, so the map and the stops list work without a connection
        private val ITINERARY_CACHE_DATE = stringPreferencesKey("itinerary_cache_date")
        private val ITINERARY_CACHE_JSON = stringPreferencesKey("itinerary_cache_json")

        // Clock-in status as last confirmed by the server
        private val CLOCKED_IN = booleanPreferencesKey("clocked_in")

        // Assignments as last downloaded, so "Assigned to You" still lists them without a connection
        private val ASSIGNMENTS_CACHE_JSON = stringPreferencesKey("assignments_cache_json")

        // Account sync: which account this device's progress belongs to, the days a report was
        // submitted, and whether something changed since the last successful upload.
        private val OWNER_USER_ID = stringPreferencesKey("owner_user_id")
        private val REPORT_DATES = stringSetPreferencesKey("report_dates")
        private val STATE_DIRTY = booleanPreferencesKey("state_dirty")

        private fun quizTimestampKey(videoId: String) = longPreferencesKey("quiz_completed_at_$videoId")

        /** Completed stops are stored as "yyyy-MM-dd:stopId" so a visit only counts for its day. */
        private fun stopKey(date: String, stopId: String) = "$date:$stopId"
    }

    // ------------------------------------------------------------------
    // Learning progress
    // ------------------------------------------------------------------

    fun getWatchedVideos(): Flow<Set<String>> =
        context.progressDataStore.data.map { it[WATCHED_VIDEOS] ?: emptySet() }

    fun getCompletedQuizzes(): Flow<Set<String>> =
        context.progressDataStore.data.map { it[COMPLETED_QUIZZES] ?: emptySet() }

    fun getCompletedLessons(): Flow<Set<String>> =
        context.progressDataStore.data.map { it[COMPLETED_LESSONS] ?: emptySet() }

    suspend fun markVideoWatched(videoId: String) {
        context.progressDataStore.edit { prefs ->
            val current = prefs[WATCHED_VIDEOS] ?: emptySet()
            prefs[WATCHED_VIDEOS] = current + videoId
        }
    }

    suspend fun markQuizCompleted(quizId: String) {
        context.progressDataStore.edit { prefs ->
            val current = prefs[COMPLETED_QUIZZES] ?: emptySet()
            prefs[COMPLETED_QUIZZES] = current + quizId
            prefs[quizTimestampKey(quizId)] = System.currentTimeMillis()
        }
    }

    suspend fun getQuizCompletionTimestamp(videoId: String): Long? {
        val prefs = context.progressDataStore.data.first()
        return prefs[quizTimestampKey(videoId)]
    }

    suspend fun markLessonCompleted(lessonId: String) {
        context.progressDataStore.edit { prefs ->
            val current = prefs[COMPLETED_LESSONS] ?: emptySet()
            prefs[COMPLETED_LESSONS] = current + lessonId
        }
    }

    // ------------------------------------------------------------------
    // Account sync
    // ------------------------------------------------------------------

    /** The account-level part of this store; emits again only when it really changes. */
    fun snapshotFlow(): Flow<LearningProgressSnapshot> =
        context.progressDataStore.data.map { prefs -> prefs.toSnapshot() }.distinctUntilChanged()

    suspend fun getSnapshot(): LearningProgressSnapshot = context.progressDataStore.data.first().toSnapshot()

    private fun Preferences.toSnapshot(): LearningProgressSnapshot {
        val date = this[DAILY_QUESTIONS_DATE]
        return LearningProgressSnapshot(
            watchedVideos = this[WATCHED_VIDEOS] ?: emptySet(),
            completedQuizzes = this[COMPLETED_QUIZZES] ?: emptySet(),
            completedLessons = this[COMPLETED_LESSONS] ?: emptySet(),
            reportDates = this[REPORT_DATES] ?: emptySet(),
            dailyQuestions = date?.let {
                DailyQuestionsResult(
                    date = it,
                    correctAnswers = this[DAILY_QUESTIONS_CORRECT] ?: 0,
                    totalQuestions = this[DAILY_QUESTIONS_TOTAL] ?: 0,
                    xpEarned = this[DAILY_QUESTIONS_XP] ?: 0
                )
            }
        )
    }

    /**
     * Merge progress that came from the server into this device. Nothing is ever removed: the id
     * sets are unioned and the daily-questions result with the later date wins.
     */
    suspend fun mergeFromServer(remote: LearningProgressSnapshot) {
        context.progressDataStore.edit { prefs ->
            prefs[WATCHED_VIDEOS] = (prefs[WATCHED_VIDEOS] ?: emptySet()) + remote.watchedVideos
            prefs[COMPLETED_QUIZZES] = (prefs[COMPLETED_QUIZZES] ?: emptySet()) + remote.completedQuizzes
            prefs[COMPLETED_LESSONS] = (prefs[COMPLETED_LESSONS] ?: emptySet()) + remote.completedLessons
            prefs[REPORT_DATES] = (prefs[REPORT_DATES] ?: emptySet()) + remote.reportDates
            val remoteDaily = remote.dailyQuestions
            val localDate = prefs[DAILY_QUESTIONS_DATE]
            if (remoteDaily != null && (localDate == null || remoteDaily.date > localDate)) {
                prefs[DAILY_QUESTIONS_DATE] = remoteDaily.date
                prefs[DAILY_QUESTIONS_CORRECT] = remoteDaily.correctAnswers
                prefs[DAILY_QUESTIONS_TOTAL] = remoteDaily.totalQuestions
                prefs[DAILY_QUESTIONS_XP] = remoteDaily.xpEarned
            }
        }
    }

    /** Remember that a daily report was submitted on [date] (kept for achievements and sync). */
    suspend fun markReportDate(date: String = DateUtils.todayIso()) {
        context.progressDataStore.edit { prefs ->
            prefs[REPORT_DATES] = (prefs[REPORT_DATES] ?: emptySet()) + date
        }
    }

    fun getReportDates(): Flow<Set<String>> =
        context.progressDataStore.data.map { it[REPORT_DATES] ?: emptySet() }

    /** Id of the account whose progress is stored on this device, or null on a fresh install. */
    suspend fun getOwnerUserId(): String? = context.progressDataStore.data.first()[OWNER_USER_ID]

    suspend fun setOwnerUserId(userId: String) {
        context.progressDataStore.edit { it[OWNER_USER_ID] = userId }
    }

    /** True while local progress has changes the server has not confirmed yet. */
    fun isStateDirty(): Flow<Boolean> =
        context.progressDataStore.data.map { it[STATE_DIRTY] ?: false }.distinctUntilChanged()

    suspend fun setStateDirty(dirty: Boolean) {
        context.progressDataStore.edit { it[STATE_DIRTY] = dirty }
    }

    /** Forget everything (used when a different account signs in on this device). */
    suspend fun clearAll() {
        context.progressDataStore.edit { it.clear() }
    }

    // ------------------------------------------------------------------
    // Daily questions (one session per day)
    // ------------------------------------------------------------------

    /** The last recorded daily-questions session, or null if none has ever been finished. */
    fun getDailyQuestionsResult(): Flow<DailyQuestionsResult?> =
        context.progressDataStore.data.map { prefs ->
            val date = prefs[DAILY_QUESTIONS_DATE] ?: return@map null
            DailyQuestionsResult(
                date = date,
                correctAnswers = prefs[DAILY_QUESTIONS_CORRECT] ?: 0,
                totalQuestions = prefs[DAILY_QUESTIONS_TOTAL] ?: 0,
                xpEarned = prefs[DAILY_QUESTIONS_XP] ?: 0
            )
        }

    /** True when today's daily questions have already been finished. */
    fun isDailyQuestionsDoneToday(): Flow<Boolean> =
        getDailyQuestionsResult().map { it?.date == DateUtils.todayIso() }

    suspend fun markDailyQuestionsCompleted(
        correctAnswers: Int,
        totalQuestions: Int,
        xpEarned: Int,
        date: String = DateUtils.todayIso()
    ) {
        context.progressDataStore.edit { prefs ->
            prefs[DAILY_QUESTIONS_DATE] = date
            prefs[DAILY_QUESTIONS_CORRECT] = correctAnswers
            prefs[DAILY_QUESTIONS_TOTAL] = totalQuestions
            prefs[DAILY_QUESTIONS_XP] = xpEarned
            prefs.remove(DAILY_SESSION_DATE)
            prefs.remove(DAILY_SESSION_ANSWERS)
        }
    }

    /**
     * The answers already given in [date]'s session. They are written the moment a question is
     * answered, because XP and lives are paid out per answer: without this, leaving the quiz
     * half-way and opening it again offered the same questions, and the same XP, once more.
     */
    suspend fun getDailySessionAnswers(date: String = DateUtils.todayIso()): List<DailyAnswer> {
        val prefs = context.progressDataStore.data.first()
        if (prefs[DAILY_SESSION_DATE] != date) return emptyList()
        return (prefs[DAILY_SESSION_ANSWERS] ?: emptySet()).mapNotNull(DailyAnswer::decode)
    }

    /** @return false when this question was already recorded for [date] (nothing is written). */
    suspend fun recordDailyAnswer(answer: DailyAnswer, date: String = DateUtils.todayIso()): Boolean {
        var added = false
        context.progressDataStore.edit { prefs ->
            val sameDay = prefs[DAILY_SESSION_DATE] == date
            val current = if (sameDay) prefs[DAILY_SESSION_ANSWERS] ?: emptySet() else emptySet()
            if (current.mapNotNull(DailyAnswer::decode).none { it.questionId == answer.questionId }) {
                prefs[DAILY_SESSION_DATE] = date
                prefs[DAILY_SESSION_ANSWERS] = current + answer.encode()
                added = true
            }
        }
        return added
    }

    // ------------------------------------------------------------------
    // Itinerary stops (date-scoped)
    // ------------------------------------------------------------------

    /** Raw set of "date:stopId" entries (plus any legacy un-dated ids). Prefer the date-scoped getters. */
    fun getCompletedStops(): Flow<Set<String>> =
        context.progressDataStore.data.map { it[COMPLETED_STOPS] ?: emptySet() }

    /** Stop ids marked visited on [date]. */
    fun getCompletedStopsForDate(date: String): Flow<Set<String>> =
        getCompletedStops().map { all ->
            val prefix = "$date:"
            all.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }.toSet()
        }

    /** Stop ids marked visited today. */
    fun getCompletedStopsToday(): Flow<Set<String>> = getCompletedStopsForDate(DateUtils.todayIso())

    suspend fun markStopCompleted(stopId: String, date: String = DateUtils.todayIso()) {
        context.progressDataStore.edit { prefs ->
            val current = prefs[COMPLETED_STOPS] ?: emptySet()
            prefs[COMPLETED_STOPS] = current + stopKey(date, stopId)
        }
    }

    /** Mark several stops visited on [date] in one write (visits the server already knows about). */
    suspend fun markStopsCompleted(stopIds: Collection<String>, date: String = DateUtils.todayIso()) {
        if (stopIds.isEmpty()) return
        context.progressDataStore.edit { prefs ->
            val current = prefs[COMPLETED_STOPS] ?: emptySet()
            prefs[COMPLETED_STOPS] = current + stopIds.map { stopKey(date, it) }
        }
    }

    suspend fun unmarkStopCompleted(stopId: String, date: String = DateUtils.todayIso()) {
        context.progressDataStore.edit { prefs ->
            val current = prefs[COMPLETED_STOPS] ?: emptySet()
            prefs[COMPLETED_STOPS] = current - stopKey(date, stopId)
        }
    }

    /** Record how many stops today's itinerary has, so the hub can show "N of M visited". */
    suspend fun setItineraryTotal(total: Int, date: String = DateUtils.todayIso()) {
        context.progressDataStore.edit { prefs ->
            prefs[ITINERARY_DATE] = date
            prefs[ITINERARY_TOTAL] = total
        }
    }

    /** Keep the route of [date] (as JSON) for use without a connection. */
    suspend fun cacheItinerary(json: String, date: String = DateUtils.todayIso()) {
        context.progressDataStore.edit { prefs ->
            prefs[ITINERARY_CACHE_DATE] = date
            prefs[ITINERARY_CACHE_JSON] = json
        }
    }

    /** The cached route of [date], or null when none was downloaded that day. */
    suspend fun getCachedItinerary(date: String = DateUtils.todayIso()): String? {
        val prefs = context.progressDataStore.data.first()
        return if (prefs[ITINERARY_CACHE_DATE] == date) prefs[ITINERARY_CACHE_JSON] else null
    }

    /** Whether the CHV is clocked in, as last confirmed by the server. False until known. */
    fun clockedIn(): Flow<Boolean> =
        context.progressDataStore.data.map { it[CLOCKED_IN] ?: false }

    suspend fun setClockedIn(clockedIn: Boolean) {
        context.progressDataStore.edit { it[CLOCKED_IN] = clockedIn }
    }

    /** Keep the last downloaded assignments (as JSON) for use without a connection. */
    suspend fun cacheAssignments(json: String) {
        context.progressDataStore.edit { it[ASSIGNMENTS_CACHE_JSON] = json }
    }

    /** The assignments as last downloaded, or null when they were never loaded on this phone. */
    suspend fun getCachedAssignments(): String? =
        context.progressDataStore.data.first()[ASSIGNMENTS_CACHE_JSON]

    /** Visited/total stops for today. total is 0 until the itinerary has been loaded once today. */
    fun getItineraryProgressToday(): Flow<ItineraryProgress> =
        context.progressDataStore.data.map { prefs ->
            val today = DateUtils.todayIso()
            val total = if (prefs[ITINERARY_DATE] == today) prefs[ITINERARY_TOTAL] ?: 0 else 0
            val prefix = "$today:"
            val visited = (prefs[COMPLETED_STOPS] ?: emptySet()).count { it.startsWith(prefix) }
            ItineraryProgress(date = today, visited = visited, total = total)
        }
}
