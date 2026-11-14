package com.afyaquest.app.presentation.learn

import androidx.annotation.StringRes
import com.afyaquest.app.R

/**
 * A focus topic of the Learn hub: a hand-picked set of videos and lessons that cuts across the
 * numbered modules (everything about maternal health, wherever it sits in the curriculum).
 *
 * Topics are plain data. To add one, add an entry here with its title/description strings.
 * [videoIds] must be ids from `VideoModulesViewModel.allVideos()` and [lessonIds] ids from
 * `LessonsViewModel.LESSON_IDS`, listed in the order they should be studied; `LearnTopicTest`
 * fails the build if an id is unknown.
 */
enum class LearnTopic(
    /** Stable id used in navigation routes. */
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val videoIds: List<String>,
    val lessonIds: List<String> = emptyList()
) {
    MATERNAL_HEALTH(
        id = "maternal-health",
        titleRes = R.string.category_maternal_health,
        descriptionRes = R.string.learn_topic_maternal_desc,
        videoIds = listOf(
            "mod1-female-reproductive",
            "mod4-antenatal-care",
            "mod4-pregnancy",
            "mod4-safe-delivery",
            "mod4-birth-spacing",
            "mod4-short-term-contraception",
            "mod4-long-term-contraception"
        ),
        lessonIds = listOf("lesson-3", "lesson-10")
    ),

    /**
     * There is no nutrition module; these are the videos the education team's own topic outlines
     * describe as covering feeding or nutrition (malnutrition; continued feeding, ORS and zinc in
     * diarrhoea; newborn feeding / exclusive breastfeeding; iron and folic acid in pregnancy),
     * plus the nutrition lessons.
     */
    NUTRITION(
        id = "nutrition",
        titleRes = R.string.category_nutrition,
        descriptionRes = R.string.learn_topic_nutrition_desc,
        videoIds = listOf(
            "mod2-malnutrition",
            "mod2-diarrhea",
            "mod4-safe-delivery",
            "mod4-antenatal-care"
        ),
        lessonIds = listOf("lesson-2", "lesson-9", "lesson-7", "lesson-8", "lesson-10")
    );

    companion object {
        fun fromId(id: String?): LearnTopic? = entries.firstOrNull { it.id == id }

        /** The first topic that contains [videoId], or null when the video is in no topic. */
        fun forVideo(videoId: String): LearnTopic? = entries.firstOrNull { videoId in it.videoIds }
    }
}

/**
 * Videos spotlighted at the top of the Learn hub and on Home, in display order. The first entry
 * is the lead video of the education section.
 */
object FeaturedLearning {
    val videoIds: List<String> = listOf(
        "mod1-female-reproductive",
        "mod4-antenatal-care",
        "mod2-malnutrition"
    )
}
