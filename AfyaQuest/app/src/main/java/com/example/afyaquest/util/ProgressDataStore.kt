package com.example.afyaquest.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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
    }

    fun getWatchedVideos(): Flow<Set<String>> =
        context.progressDataStore.data.map { preferences ->
            preferences[WATCHED_VIDEOS] ?: emptySet()
        }

    fun getCompletedQuizzes(): Flow<Set<String>> =
        context.progressDataStore.data.map { preferences ->
            preferences[COMPLETED_QUIZZES] ?: emptySet()
        }

    fun getCompletedLessons(): Flow<Set<String>> =
        context.progressDataStore.data.map { preferences ->
            preferences[COMPLETED_LESSONS] ?: emptySet()
        }

    fun getCompletedStops(): Flow<Set<String>> =
        context.progressDataStore.data.map { preferences ->
            preferences[COMPLETED_STOPS] ?: emptySet()
        }

    suspend fun markVideoWatched(videoId: String) {
        context.progressDataStore.edit { preferences ->
            val current = preferences[WATCHED_VIDEOS] ?: emptySet()
            preferences[WATCHED_VIDEOS] = current + videoId
        }
    }

    suspend fun markQuizCompleted(quizId: String) {
        context.progressDataStore.edit { preferences ->
            val current = preferences[COMPLETED_QUIZZES] ?: emptySet()
            preferences[COMPLETED_QUIZZES] = current + quizId
        }
    }

    suspend fun markLessonCompleted(lessonId: String) {
        context.progressDataStore.edit { preferences ->
            val current = preferences[COMPLETED_LESSONS] ?: emptySet()
            preferences[COMPLETED_LESSONS] = current + lessonId
        }
    }

    suspend fun markStopCompleted(stopId: String) {
        context.progressDataStore.edit { preferences ->
            val current = preferences[COMPLETED_STOPS] ?: emptySet()
            preferences[COMPLETED_STOPS] = current + stopId
        }
    }
}
