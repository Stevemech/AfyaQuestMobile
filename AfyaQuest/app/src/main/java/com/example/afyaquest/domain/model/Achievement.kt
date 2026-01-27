package com.example.afyaquest.domain.model

/**
 * Achievement badge model
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String, // Emoji
    val category: AchievementCategory,
    val unlocked: Boolean,
    val unlockedDate: String? = null,
    val progress: Int = 0, // For progress-based achievements
    val target: Int = 0 // Target value for completion
)

enum class AchievementCategory {
    LEARNING,
    CONSISTENCY,
    COMMUNITY,
    EXPERTISE,
    MILESTONES
}

/**
 * Weekly reflection model
 */
data class WeeklyReflection(
    val id: String,
    val weekStartDate: String,
    val weekEndDate: String,
    val successStory: String,
    val challengesFaced: String,
    val lessonsLearned: String,
    val goalsNextWeek: String,
    val overallRating: Int, // 1-5 stars
    val submittedDate: String
)
