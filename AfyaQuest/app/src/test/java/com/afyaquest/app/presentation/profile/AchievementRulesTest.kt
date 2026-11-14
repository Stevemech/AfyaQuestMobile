package com.afyaquest.app.presentation.profile

import com.afyaquest.app.domain.model.AchievementCategory
import com.afyaquest.app.presentation.learn.LearnTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementRulesTest {

    private fun status(id: String, input: AchievementInput) =
        AchievementRules.evaluate(input).first { it.definition.id == id }

    @Test
    fun `definitions are complete and unique`() {
        val definitions = AchievementRules.definitions
        assertEquals(definitions.size, definitions.map { it.id }.toSet().size)
        definitions.forEach { assertTrue("${it.id} needs a positive target", it.target > 0) }
        val categories = definitions.map { it.category }.toSet()
        assertTrue(categories.containsAll(listOf(
            AchievementCategory.LEARNING, AchievementCategory.CONSISTENCY, AchievementCategory.COMMUNITY,
            AchievementCategory.EXPERTISE, AchievementCategory.MILESTONES
        )))
    }

    @Test
    fun `a new account has nothing unlocked and no progress`() {
        val all = AchievementRules.evaluate(AchievementInput())
        assertEquals(AchievementRules.definitions.size, all.size)
        assertTrue(all.none { it.unlocked })
        assertTrue(all.all { it.progress == 0 })
    }

    @Test
    fun `watching videos unlocks the video achievements in turn`() {
        val one = AchievementInput(watchedVideos = setOf("mod1-female-reproductive"))
        assertTrue(status("first-video", one).unlocked)
        assertFalse(status("video-10", one).unlocked)
        assertEquals(1, status("video-10", one).progress)

        val ten = AchievementInput(watchedVideos = (1..12).map { "video-$it" }.toSet())
        assertTrue(status("video-10", ten).unlocked)
        // Progress is capped at the target so the card never reads 12/10.
        assertEquals(10, status("video-10", ten).progress)
    }

    @Test
    fun `quizzes lessons and reports count towards their own achievements`() {
        val input = AchievementInput(
            completedQuizzes = (1..5).map { "quiz-$it" }.toSet(),
            completedLessons = setOf("lesson-1", "lesson-2"),
            reportDays = 1
        )
        assertTrue(status("quiz-5", input).unlocked)
        assertEquals(2, status("lessons-5", input).progress)
        assertFalse(status("lessons-5", input).unlocked)
        assertTrue(status("report-1", input).unlocked)
        assertEquals(1, status("report-5", input).progress)
    }

    @Test
    fun `a topic achievement needs every video and lesson of that topic`() {
        val topic = LearnTopic.NUTRITION
        val allButOne = AchievementInput(
            watchedVideos = topic.videoIds.toSet(),
            completedLessons = topic.lessonIds.drop(1).toSet()
        )
        val almost = status("topic-nutrition", allButOne)
        assertFalse(almost.unlocked)
        assertEquals(almost.definition.target - 1, almost.progress)

        val everything = AchievementInput(
            watchedVideos = topic.videoIds.toSet() + "mod1-nervous-system",
            completedLessons = topic.lessonIds.toSet() + "lesson-1"
        )
        assertTrue(status("topic-nutrition", everything).unlocked)
        // Unrelated content does not count towards another topic.
        assertFalse(status("topic-maternal-health", everything).unlocked)
    }

    @Test
    fun `streak achievements use the best streak ever, so they do not re-lock`() {
        assertFalse(status("streak-3", AchievementInput(longestStreak = 2)).unlocked)
        val week = AchievementInput(longestStreak = 7)
        assertTrue(status("streak-3", week).unlocked)
        assertTrue(status("streak-7", week).unlocked)
    }

    @Test
    fun `level and XP milestones`() {
        val input = AchievementInput(totalXP = 520, level = 3)
        assertTrue(status("level-3", input).unlocked)
        assertFalse(status("xp-1000", input).unlocked)
        assertEquals(520, status("xp-1000", input).progress)
        assertTrue(status("xp-1000", AchievementInput(totalXP = 4000, level = 8)).unlocked)
    }
}
