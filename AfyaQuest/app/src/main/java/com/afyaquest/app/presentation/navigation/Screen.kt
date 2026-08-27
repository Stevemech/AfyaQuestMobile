package com.afyaquest.app.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
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
    object VideoPlayer : Screen("video_player/{moduleId}") {
        fun createRoute(moduleId: String) = "video_player/$moduleId"
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
