package com.example.afyaquest.presentation.videomodules

import androidx.lifecycle.ViewModel
import com.example.afyaquest.domain.model.VideoCategory
import com.example.afyaquest.domain.model.VideoModule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for Video Modules screen
 */
@HiltViewModel
class VideoModulesViewModel @Inject constructor(
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
        // Sample video data
        _videos.value = listOf(
            VideoModule(
                id = "1",
                title = "Module 1: Health Assessments",
                description = "Learn how to conduct comprehensive health assessments using the Medical Detective's Handbook",
                thumbnail = "🎬",
                duration = "6:50",
                category = VideoCategory.BASICS,
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "2",
                title = "Module 2: Water Sanitation Practices",
                description = "Understanding water treatment and safe storage",
                thumbnail = "💧",
                duration = "15:45",
                category = VideoCategory.SANITATION,
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "3",
                title = "Module 3: Maternal and Child Health",
                description = "Essential care for mothers and children",
                thumbnail = "👶",
                duration = "20:15",
                category = VideoCategory.MATERNAL,
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "4",
                title = "Module 4: Vaccination Programs",
                description = "Understanding vaccination schedules and importance",
                thumbnail = "💉",
                duration = "18:00",
                category = VideoCategory.IMMUNIZATION,
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "5",
                title = "Module 5: Emergency First Aid",
                description = "Learn essential emergency first aid techniques for common medical situations",
                thumbnail = "🚨",
                duration = "7:21",
                category = VideoCategory.EMERGENCY,
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "6",
                title = "Module 6: Nutrition Basics",
                description = "Understanding balanced nutrition and dietary needs",
                thumbnail = "🥗",
                duration = "12:30",
                category = VideoCategory.NUTRITION,
                hasQuiz = true,
                watched = false
            ),
            VideoModule(
                id = "7",
                title = "Module 7: Disease Prevention",
                description = "Common diseases and prevention strategies",
                thumbnail = "🛡️",
                duration = "14:15",
                category = VideoCategory.DISEASE_PREVENTION,
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
    fun getWatchedCount(): Int = _watchedVideos.value.size
    fun getQuizCompletedCount(): Int = _completedQuizzes.value.size
    fun getTotalVideos(): Int = _videos.value.size

    /**
     * Get category display name
     */
    fun getCategoryDisplayName(category: VideoCategory): String {
        return when (category) {
            VideoCategory.BASICS -> "Health Assessment"
            VideoCategory.SANITATION -> "Sanitation"
            VideoCategory.MATERNAL -> "Maternal Health"
            VideoCategory.IMMUNIZATION -> "Immunization"
            VideoCategory.EMERGENCY -> "Emergency"
            VideoCategory.NUTRITION -> "Nutrition"
            VideoCategory.DISEASE_PREVENTION -> "Disease Prevention"
        }
    }
}
