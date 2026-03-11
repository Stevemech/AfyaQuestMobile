package com.example.afyaquest.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object DailyQuestions : Screen("daily_questions")
    object DailyReport : Screen("daily_report")
    object VideoModules : Screen("video_modules")
    object ModuleDetail : Screen("module_detail/{moduleNumber}") {
        fun createRoute(moduleNumber: Int) = "module_detail/$moduleNumber"
    }
    object Lessons : Screen("lessons")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object Chat : Screen("chat")
    object Map : Screen("map")
    object ModuleQuiz : Screen("module_quiz/{moduleId}") {
        fun createRoute(moduleId: String) = "module_quiz/$moduleId"
    }
    object VideoPlayer : Screen("video_player/{moduleId}") {
        fun createRoute(moduleId: String) = "video_player/$moduleId"
    }
    object Assignments : Screen("assignments")
}
