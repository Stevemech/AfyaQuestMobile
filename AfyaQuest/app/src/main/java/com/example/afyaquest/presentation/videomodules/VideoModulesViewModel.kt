package com.example.afyaquest.presentation.videomodules

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afyaquest.R
import com.example.afyaquest.data.remote.ApiService
import com.example.afyaquest.domain.model.VideoCategory
import com.example.afyaquest.domain.model.VideoModule
import com.example.afyaquest.util.ProgressDataStore
import com.example.afyaquest.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Video Modules screen.
 * Persists watched/quiz-complete state via DataStore and syncs to AWS.
 */
@HiltViewModel
class VideoModulesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val progressDataStore: ProgressDataStore,
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoModule>>(emptyList())
    val videos: StateFlow<List<VideoModule>> = _videos.asStateFlow()

    private val _selectedCategory = MutableStateFlow<VideoCategory?>(null)
    val selectedCategory: StateFlow<VideoCategory?> = _selectedCategory.asStateFlow()

    private val _watchedVideos = MutableStateFlow<Set<String>>(emptySet())
    val watchedVideos: StateFlow<Set<String>> = _watchedVideos.asStateFlow()

    private val _completedQuizzes = MutableStateFlow<Set<String>>(emptySet())
    val completedQuizzes: StateFlow<Set<String>> = _completedQuizzes.asStateFlow()

    val categories = listOf(
        VideoCategory.BASICS
    )

    init {
        loadVideos()
        loadSavedProgress()
    }

    private fun loadVideos() {
        _videos.value = listOf(
            VideoModule(
                id = "8",
                title = context.getString(R.string.video_module_8_title),
                description = context.getString(R.string.video_module_8_desc),
                thumbnail = "🔬",
                duration = "",
                category = VideoCategory.BASICS,
                videoUrl = "https://afyaquest-module-videos.s3.af-south-1.amazonaws.com/Male+Reproductive+System+(1).mp4",
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "9",
                title = context.getString(R.string.video_module_9_title),
                description = context.getString(R.string.video_module_9_desc),
                thumbnail = "🔬",
                duration = "",
                category = VideoCategory.BASICS,
                videoUrl = "https://afyaquest-module-videos.s3.af-south-1.amazonaws.com/Female+Reproductive+System.mp4",
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "10",
                title = context.getString(R.string.video_module_10_title),
                description = context.getString(R.string.video_module_10_desc),
                thumbnail = "🔬",
                duration = "",
                category = VideoCategory.BASICS,
                videoUrl = "https://afyaquest-module-videos.s3.af-south-1.amazonaws.com/Urinary+System.mov",
                hasQuiz = true,
                watched = false
            )
        )
    }

    /**
     * Load saved progress from DataStore so it persists across navigation
     */
    private fun loadSavedProgress() {
        viewModelScope.launch {
            progressDataStore.getWatchedVideos().collect { watched ->
                _watchedVideos.value = watched
            }
        }
        viewModelScope.launch {
            progressDataStore.getCompletedQuizzes().collect { completed ->
                _completedQuizzes.value = completed
            }
        }
    }

    fun getFilteredVideos(): List<VideoModule> {
        val category = _selectedCategory.value
        val allVideos = _videos.value

        return if (category == null) {
            allVideos
        } else {
            allVideos.filter { it.category == category }
        }.map { video ->
            video.copy(
                watched = _watchedVideos.value.contains(video.id),
                quizComplete = _completedQuizzes.value.contains(video.id)
            )
        }
    }

    fun setCategory(category: VideoCategory?) {
        _selectedCategory.value = category
    }

    /**
     * Mark video as watched — persists locally and syncs to AWS
     */
    fun markVideoWatched(videoId: String) {
        if (_watchedVideos.value.contains(videoId)) return
        _watchedVideos.value = _watchedVideos.value + videoId
        viewModelScope.launch {
            progressDataStore.markVideoWatched(videoId)
            syncProgressToApi("module_watched", videoId)
        }
    }

    /**
     * Mark quiz as completed — persists locally and syncs to AWS
     */
    fun markQuizCompleted(videoId: String) {
        if (_completedQuizzes.value.contains(videoId)) return
        _completedQuizzes.value = _completedQuizzes.value + videoId
        viewModelScope.launch {
            progressDataStore.markQuizCompleted(videoId)
            syncProgressToApi("module_quiz_complete", videoId)
        }
    }

    /**
     * Sync progress update to the backend (best-effort, non-blocking)
     */
    private suspend fun syncProgressToApi(type: String, itemId: String) {
        try {
            val token = tokenManager.getIdToken() ?: return
            val body = mapOf<String, Any>(
                "type" to type,
                "itemId" to itemId
            )
            apiService.updateUserProgress("Bearer $token", body)
        } catch (e: Exception) {
            Log.d("VideoModulesVM", "Progress sync failed (will retry): ${e.message}")
        }
    }

    fun getVideoUrl(videoId: String): String? =
        _videos.value.find { it.id == videoId }?.videoUrl

    fun getLocalFilePath(videoId: String): String? =
        _videos.value.find { it.id == videoId }?.localFilePath

    fun getVideoTitle(videoId: String): String =
        _videos.value.find { it.id == videoId }?.title ?: "Video"

    fun getWatchedCount(): Int = _watchedVideos.value.size
    fun getQuizCompletedCount(): Int = _completedQuizzes.value.size
    fun getTotalVideos(): Int = _videos.value.size

    fun getCategoryDisplayName(category: VideoCategory): String {
        return when (category) {
            VideoCategory.BASICS -> context.getString(R.string.video_category_basics)
            VideoCategory.SANITATION -> context.getString(R.string.video_category_sanitation)
            VideoCategory.MATERNAL -> context.getString(R.string.video_category_maternal)
            VideoCategory.IMMUNIZATION -> context.getString(R.string.video_category_immunization)
            VideoCategory.EMERGENCY -> context.getString(R.string.video_category_emergency)
            VideoCategory.NUTRITION -> context.getString(R.string.video_category_nutrition)
            VideoCategory.DISEASE_PREVENTION -> context.getString(R.string.video_category_disease_prevention)
        }
    }
}
