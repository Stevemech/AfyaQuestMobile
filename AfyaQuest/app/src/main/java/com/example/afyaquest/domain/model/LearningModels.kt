package com.example.afyaquest.domain.model

/**
 * Video module for learning
 */
data class VideoModule(
    val id: String,
    val title: String,
    val description: String,
    val thumbnail: String = "", // Emoji or URL
    val duration: String, // e.g., "6:50"
    val category: VideoCategory,
    val videoUrl: String? = null, // CloudFront URL
    val s3Key: String? = null,
    val hasQuiz: Boolean = false,
    val watched: Boolean = false,
    val quizComplete: Boolean = false
)

enum class VideoCategory {
    BASICS,
    SANITATION,
    MATERNAL,
    IMMUNIZATION,
    EMERGENCY,
    NUTRITION,
    DISEASE_PREVENTION
}

/**
 * Interactive lesson
 */
data class Lesson(
    val id: String,
    val title: String,
    val description: String,
    val category: LessonCategory,
    val difficulty: Difficulty,
    val content: String, // Markdown or HTML content
    val estimatedMinutes: Int,
    val points: Int,
    val completed: Boolean = false
)

enum class LessonCategory {
    HYGIENE,
    NUTRITION,
    MATERNAL_HEALTH,
    CHILD_CARE,
    DISEASE_PREVENTION,
    FIRST_AID,
    MEDICATION,
    HEALTH_EDUCATION
}

/**
 * Module quiz question
 */
data class ModuleQuizQuestion(
    val id: String,
    val moduleId: String,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)

/**
 * Video modules response
 */
data class VideoModulesResponse(
    val videos: List<VideoModule>
)

/**
 * Lessons response
 */
data class LessonsResponse(
    val lessons: List<Lesson>
)
