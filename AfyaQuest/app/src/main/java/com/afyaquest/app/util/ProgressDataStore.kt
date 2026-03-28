package com.afyaquest.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(name = "learning_progress")

@Singleton
class ProgressDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val WATCHED_VIDEOS = stringSetPreferencesKey("watched_videos")
        private val COMPLETED_QUIZZES = stringSetPreferencesKey("completed_quizzes")
        private val COMPLETED_LESSONS = stringSetPreferencesKey("completed_lessons")
        private val COMPLETED_STOPS = stringSetPreferencesKey("completed_stops")
        private fun quizTimestampKey(videoId: String) = longPreferencesKey("quiz_completed_at_$videoId")
    }

    fun getWatchedVideos(): Flow<Set<String>> =
        context.progressDataStore.data.map { it[WATCHED_VIDEOS] ?: emptySet() }

    fun getCompletedQuizzes(): Flow<Set<String>> =
        context.progressDataStore.data.map { it[COMPLETED_QUIZZES] ?: emptySet() }

    fun getCompletedLessons(): Flow<Set<String>> =
        context.progressDataStore.data.map { it[COMPLETED_LESSONS] ?: emptySet() }

    fun getCompletedStops(): Flow<Set<String>> =
        context.progressDataStore.data.map { it[COMPLETED_STOPS] ?: emptySet() }

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

    suspend fun markStopCompleted(stopId: String) {
        context.progressDataStore.edit { prefs ->
            val current = prefs[COMPLETED_STOPS] ?: emptySet()
            prefs[COMPLETED_STOPS] = current + stopId
        }
    }
}
