package com.example.afyaquest.presentation.navigation

/**
 * Sealed class representing app navigation destinations.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object DailyQuestions : Screen("daily_questions")
    object DailyReport : Screen("daily_report")
    object VideoModules : Screen("video_modules")
    object Lessons : Screen("lessons")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object Chat : Screen("chat")
    object Map : Screen("map")
}
