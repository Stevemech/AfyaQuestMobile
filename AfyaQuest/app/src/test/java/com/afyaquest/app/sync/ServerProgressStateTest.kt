package com.afyaquest.app.sync

import com.afyaquest.app.util.DailyQuestionsResult
import com.afyaquest.app.util.LearningProgressSnapshot
import com.afyaquest.app.util.XpManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerProgressStateTest {

    @Test
    fun `decodes the state the server sends (JSON numbers arrive as doubles)`() {
        val state = ServerProgressState.fromMap(
            mapOf(
                "totalPoints" to 245.0,
                "lives" to 6.0,
                "currentStreak" to 4.0,
                "lastStreakDate" to "2026-11-12",
                "longestStreak" to 9.0,
                "watchedVideos" to listOf("mod1-female-reproductive", "mod2-malnutrition"),
                "completedQuizzes" to listOf("mod2-malnutrition"),
                "completedLessons" to listOf("lesson-2"),
                "reportDates" to listOf("2026-11-11", "2026-11-12"),
                "dailyQuestions" to mapOf("date" to "2026-11-12", "correct" to 3.0, "total" to 3.0, "xp" to 85.0),
                "level" to 1.0,
                "rank" to "Beginner"
            )
        )!!

        assertEquals(245, state.totalPoints)
        assertEquals(6, state.lives)
        assertEquals(4, state.currentStreak)
        assertEquals("2026-11-12", state.lastStreakDate)
        assertEquals(9, state.longestStreak)
        assertEquals(setOf("mod1-female-reproductive", "mod2-malnutrition"), state.progress.watchedVideos)
        assertEquals(setOf("2026-11-11", "2026-11-12"), state.progress.reportDates)
        assertEquals(DailyQuestionsResult("2026-11-12", 3, 3, 85), state.progress.dailyQuestions)
    }

    @Test
    fun `missing or malformed fields fall back to an empty state instead of failing`() {
        val state = ServerProgressState.fromMap(
            mapOf("totalPoints" to "many", "watchedVideos" to "not-a-list", "completedLessons" to listOf(1.0, "lesson-1", null))
        )!!

        assertEquals(0, state.totalPoints)
        assertNull(state.lives)
        assertEquals("", state.lastStreakDate)
        assertEquals(emptySet<String>(), state.progress.watchedVideos)
        assertEquals(setOf("lesson-1"), state.progress.completedLessons)
        assertNull(state.progress.dailyQuestions)
        assertNull(ServerProgressState.fromMap(null))
    }

    @Test
    fun `the upload carries the whole account state in a stable order`() {
        val body = ServerProgressState.uploadBody(
            XpManager.SyncState(totalXP = 320, lives = 8, storedStreak = 3, lastStreakDate = "2026-11-12", longestStreak = 5),
            LearningProgressSnapshot(
                watchedVideos = setOf("mod4-pregnancy", "mod1-female-reproductive"),
                completedQuizzes = setOf("mod4-pregnancy"),
                completedLessons = setOf("lesson-8", "lesson-10"),
                reportDates = setOf("2026-11-12"),
                dailyQuestions = DailyQuestionsResult("2026-11-12", 1, 3, 45)
            )
        )

        assertEquals("state_sync", body["type"])
        @Suppress("UNCHECKED_CAST")
        val state = body["state"] as Map<String, Any>
        assertEquals(320, state["totalPoints"])
        assertEquals(8, state["lives"])
        assertEquals(3, state["currentStreak"])
        assertEquals(5, state["longestStreak"])
        assertEquals(listOf("mod1-female-reproductive", "mod4-pregnancy"), state["watchedVideos"])
        assertEquals(listOf("lesson-10", "lesson-8"), state["completedLessons"])
        assertEquals(mapOf("date" to "2026-11-12", "correct" to 1, "total" to 3, "xp" to 45), state["dailyQuestions"])
    }

    @Test
    fun `no daily questions entry is sent before the first session`() {
        val body = ServerProgressState.uploadBody(XpManager.SyncState(), LearningProgressSnapshot())

        @Suppress("UNCHECKED_CAST")
        val state = body["state"] as Map<String, Any>
        assertEquals(false, state.containsKey("dailyQuestions"))
        assertEquals(emptyList<String>(), state["watchedVideos"])
    }
}
