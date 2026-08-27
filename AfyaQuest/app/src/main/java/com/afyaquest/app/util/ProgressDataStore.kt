package com.afyaquest.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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

        // Itinerary: how many stops today's route has (so the hub can show N of M)
        private val ITINERARY_DATE = stringPreferencesKey("itinerary_date")
        private val ITINERARY_TOTAL = intPreferencesKey("itinerary_total")

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
        }
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
