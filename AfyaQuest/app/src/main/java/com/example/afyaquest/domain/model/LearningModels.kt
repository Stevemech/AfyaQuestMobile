package com.example.afyaquest.domain.model

data class VideoModuleFolder(
    val moduleNumber: Int,
    val title: String,
    val description: String,
    val icon: String,
    val videoCount: Int,
    val watchedCount: Int = 0,
    val quizzesCompleted: Int = 0
)

data class VideoModule(
    val id: String,
    val moduleNumber: Int,
    val title: String,
    val description: String,
    val thumbnail: String = "",
    val duration: String,
    val category: VideoCategory,
    val videoUrl: String? = null,
    val s3Key: String? = null,
    val localFilePath: String? = null,
    val hasQuiz: Boolean = false,
    val watched: Boolean = false,
    val quizComplete: Boolean = false,
    val isDownloaded: Boolean = false
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

data class Lesson(
    val id: String,
    val title: String,
    val description: String,
    val category: LessonCategory,
    val difficulty: Difficulty,
    val content: String,
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

data class ModuleQuizQuestion(
    val id: String,
    val moduleId: String,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)

data class VideoModulesResponse(
    val videos: List<VideoModule>
)

data class LessonsResponse(
    val lessons: List<Lesson>
)
