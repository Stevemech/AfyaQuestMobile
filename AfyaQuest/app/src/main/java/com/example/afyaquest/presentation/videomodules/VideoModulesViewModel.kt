package com.example.afyaquest.presentation.videomodules

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.afyaquest.R
import com.example.afyaquest.domain.model.VideoCategory
import com.example.afyaquest.domain.model.VideoModule
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for Video Modules screen
 */
@HiltViewModel
class VideoModulesViewModel @Inject constructor(
    @ApplicationContext private val context: Context
    // TODO: Inject VideosRepository when backend is ready
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
        VideoCategory.BASICS,
        VideoCategory.SANITATION,
        VideoCategory.MATERNAL,
        VideoCategory.IMMUNIZATION,
        VideoCategory.EMERGENCY,
        VideoCategory.NUTRITION,
        VideoCategory.DISEASE_PREVENTION
    )

    init {
        loadVideos()
    }

    /**
     * Load video modules
     * In production, this would fetch from API
     */
    private fun loadVideos() {
        // Sample video data - titles and descriptions from localized strings
        _videos.value = listOf(
            VideoModule(
                id = "1",
                title = context.getString(R.string.video_module_1_title),
                description = context.getString(R.string.video_module_1_desc),
                thumbnail = "🎬",
                duration = "6:50",
                category = VideoCategory.BASICS,
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "2",
                title = context.getString(R.string.video_module_2_title),
                description = context.getString(R.string.video_module_2_desc),
                thumbnail = "💧",
                duration = "15:45",
                category = VideoCategory.SANITATION,
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "3",
                title = context.getString(R.string.video_module_3_title),
                description = context.getString(R.string.video_module_3_desc),
                thumbnail = "👶",
                duration = "20:15",
                category = VideoCategory.MATERNAL,
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "4",
                title = context.getString(R.string.video_module_4_title),
                description = context.getString(R.string.video_module_4_desc),
                thumbnail = "💉",
                duration = "18:00",
                category = VideoCategory.IMMUNIZATION,
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "5",
                title = context.getString(R.string.video_module_5_title),
                description = context.getString(R.string.video_module_5_desc),
                thumbnail = "🚨",
                duration = "7:21",
                category = VideoCategory.EMERGENCY,
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "6",
                title = context.getString(R.string.video_module_6_title),
                description = context.getString(R.string.video_module_6_desc),
                thumbnail = "🥗",
                duration = "12:30",
                category = VideoCategory.NUTRITION,
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "7",
                title = context.getString(R.string.video_module_7_title),
                description = context.getString(R.string.video_module_7_desc),
                thumbnail = "🛡️",
                duration = "14:15",
                category = VideoCategory.DISEASE_PREVENTION,
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "8",
                title = "Module 8: Male Reproductive System",
                description = "Understanding the anatomy and function of the male reproductive system",
                thumbnail = "🔬",
                duration = "",
                category = VideoCategory.BASICS,
                videoUrl = "https://afyaquest-module-videos.s3.af-south-1.amazonaws.com/Male+Reproductive+System+(1).mp4",
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "9",
                title = "Module 9: Female Reproductive System",
                description = "Understanding the anatomy and function of the female reproductive system",
                thumbnail = "🔬",
                duration = "",
                category = VideoCategory.BASICS,
                videoUrl = "https://afyaquest-module-videos.s3.af-south-1.amazonaws.com/Female+Reproductive+System.mp4",
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "10",
                title = "Module 10: Urinary System",
                description = "Understanding the organs, functions, and common conditions of the urinary system",
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
     * Get filtered videos by category
     */
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

    /**
     * Set category filter
     */
    fun setCategory(category: VideoCategory?) {
        _selectedCategory.value = category
    }

    /**
     * Mark video as watched
     */
    fun markVideoWatched(videoId: String) {
        _watchedVideos.value = _watchedVideos.value + videoId
    }

    /**
     * Mark quiz as completed
     */
    fun markQuizCompleted(videoId: String) {
        _completedQuizzes.value = _completedQuizzes.value + videoId
    }

    /**
     * Get stats
     */
    fun getVideoUrl(videoId: String): String? =
        _videos.value.find { it.id == videoId }?.videoUrl

    fun getVideoTitle(videoId: String): String =
        _videos.value.find { it.id == videoId }?.title ?: "Video"

    fun getWatchedCount(): Int = _watchedVideos.value.size
    fun getQuizCompletedCount(): Int = _completedQuizzes.value.size
    fun getTotalVideos(): Int = _videos.value.size

    /**
     * Get category display name
     */
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
