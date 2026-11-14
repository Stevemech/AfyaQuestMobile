package com.afyaquest.app.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")

    /** Home hub. Root of the post-login back stack and the anchor for bottom-bar navigation. */
    object Dashboard : Screen("dashboard")

    object DailyQuestions : Screen("daily_questions")
    object DailyReport : Screen("daily_report")
    object Map : Screen("map")

    /**
     * Learning hub with two tabs (video modules / lessons).
     * Replaces the former separate video_modules and lessons destinations.
     */
    object Learn : Screen("learn?tab={tab}") {
        const val ARG_TAB = "tab"
        const val TAB_VIDEOS = "videos"
        const val TAB_LESSONS = "lessons"
        fun createRoute(tab: String = TAB_VIDEOS) = "learn?tab=$tab"
    }

    object ModuleDetail : Screen("module_detail/{moduleNumber}") {
        fun createRoute(moduleNumber: Int) = "module_detail/$moduleNumber"
    }
    /** A focus topic (see LearnTopic): its videos and lessons across all modules. */
    object Topic : Screen("learn_topic/{topicId}") {
        const val ARG_TOPIC_ID = "topicId"
        fun createRoute(topicId: String) = "learn_topic/$topicId"
    }

    /**
     * Video player. The optional topic argument keeps "Next video" inside the focus topic the
     * video was opened from instead of its numbered module.
     */
    object VideoPlayer : Screen("video_player/{moduleId}?topic={topic}") {
        const val ARG_TOPIC = "topic"
        fun createRoute(moduleId: String, topicId: String? = null) =
            if (topicId.isNullOrBlank()) "video_player/$moduleId" else "video_player/$moduleId?topic=$topicId"
    }
    object ModuleQuiz : Screen("module_quiz/{moduleId}") {
        fun createRoute(moduleId: String) = "module_quiz/$moduleId"
    }
    object LessonDetail : Screen("lesson/{lessonId}") {
        fun createRoute(lessonId: String) = "lesson/$lessonId"
    }

    object Assignments : Screen("assignments")
    object Chat : Screen("chat")
    object Profile : Screen("profile")
    object Settings : Screen("settings")

    // Emergency Response Guide
    object EmergencyGuide : Screen("emergency_guide")
    object TriageFlow : Screen("triage_flow")
    object CaseHistory : Screen("case_history")
}
