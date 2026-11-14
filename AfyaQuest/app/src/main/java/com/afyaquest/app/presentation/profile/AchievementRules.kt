package com.afyaquest.app.presentation.profile

import androidx.annotation.StringRes
import com.afyaquest.app.R
import com.afyaquest.app.domain.model.AchievementCategory
import com.afyaquest.app.presentation.learn.LearnTopic

/** The account progress achievements are worked out from. Every value only ever grows. */
data class AchievementInput(
    val watchedVideos: Set<String> = emptySet(),
    val completedQuizzes: Set<String> = emptySet(),
    val completedLessons: Set<String> = emptySet(),
    /** Number of different days a daily report was submitted. */
    val reportDays: Int = 0,
    val totalXP: Int = 0,
    val level: Int = 0,
    val longestStreak: Int = 0
)

/** One achievement: what it is called and how far [AchievementInput] is towards it. */
data class AchievementDefinition(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    /** Emoji shown on the card. */
    val icon: String,
    val category: AchievementCategory,
    val target: Int,
    val progress: (AchievementInput) -> Int
)

/** Where a user stands on one achievement. [progress] never exceeds [target]. */
data class AchievementStatus(
    val definition: AchievementDefinition,
    val progress: Int,
    val unlocked: Boolean
)

/**
 * The achievements of the Profile screen.
 *
 * They are computed on the device from the account's real progress (the same state that is synced
 * with the server), so they work offline, follow the user to a new phone, and can never show
 * something the user has not done. To add one, add a definition and its two strings.
 */
object AchievementRules {

    private fun topicProgress(topic: LearnTopic, input: AchievementInput): Int =
        topic.videoIds.count { it in input.watchedVideos } + topic.lessonIds.count { it in input.completedLessons }

    private fun topicSize(topic: LearnTopic): Int = topic.videoIds.size + topic.lessonIds.size

    val definitions: List<AchievementDefinition> = listOf(
        // Learning
        AchievementDefinition(
            "first-video", R.string.achievement_first_video_title, R.string.achievement_first_video_desc,
            "🎬", AchievementCategory.LEARNING, 1
        ) { it.watchedVideos.size },
        AchievementDefinition(
            "video-10", R.string.achievement_video_10_title, R.string.achievement_video_10_desc,
            "📺", AchievementCategory.LEARNING, 10
        ) { it.watchedVideos.size },
        AchievementDefinition(
            "quiz-5", R.string.achievement_quiz_5_title, R.string.achievement_quiz_5_desc,
            "🧠", AchievementCategory.LEARNING, 5
        ) { it.completedQuizzes.size },
        AchievementDefinition(
            "lessons-5", R.string.achievement_lessons_5_title, R.string.achievement_lessons_5_desc,
            "📚", AchievementCategory.LEARNING, 5
        ) { it.completedLessons.size },

        // Expertise: a whole focus topic finished
        AchievementDefinition(
            "topic-maternal-health", R.string.achievement_topic_maternal_title, R.string.achievement_topic_maternal_desc,
            "🤰", AchievementCategory.EXPERTISE, topicSize(LearnTopic.MATERNAL_HEALTH)
        ) { topicProgress(LearnTopic.MATERNAL_HEALTH, it) },
        AchievementDefinition(
            "topic-nutrition", R.string.achievement_topic_nutrition_title, R.string.achievement_topic_nutrition_desc,
            "🥗", AchievementCategory.EXPERTISE, topicSize(LearnTopic.NUTRITION)
        ) { topicProgress(LearnTopic.NUTRITION, it) },

        // Consistency
        AchievementDefinition(
            "streak-3", R.string.achievement_streak_3_title, R.string.achievement_streak_3_desc,
            "🔥", AchievementCategory.CONSISTENCY, 3
        ) { it.longestStreak },
        AchievementDefinition(
            "streak-7", R.string.achievement_streak_7_title, R.string.achievement_streak_7_desc,
            "🏅", AchievementCategory.CONSISTENCY, 7
        ) { it.longestStreak },

        // Community: field work reported
        AchievementDefinition(
            "report-1", R.string.achievement_report_1_title, R.string.achievement_report_1_desc,
            "📝", AchievementCategory.COMMUNITY, 1
        ) { it.reportDays },
        AchievementDefinition(
            "report-5", R.string.achievement_report_5_title, R.string.achievement_report_5_desc,
            "📋", AchievementCategory.COMMUNITY, 5
        ) { it.reportDays },

        // Milestones
        AchievementDefinition(
            "level-3", R.string.achievement_level_3_title, R.string.achievement_level_3_desc,
            "⭐", AchievementCategory.MILESTONES, 3
        ) { it.level },
        AchievementDefinition(
            "xp-1000", R.string.achievement_xp_1000_title, R.string.achievement_xp_1000_desc,
            "💎", AchievementCategory.MILESTONES, 1000
        ) { it.totalXP }
    )

    /** Status of every achievement for [input], in definition order. */
    fun evaluate(input: AchievementInput): List<AchievementStatus> = definitions.map { definition ->
        val progress = definition.progress(input).coerceIn(0, definition.target)
        AchievementStatus(definition = definition, progress = progress, unlocked = progress >= definition.target)
    }
}
