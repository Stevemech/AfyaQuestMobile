package com.example.afyaquest.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.afyaquest.presentation.auth.LoginScreen
import com.example.afyaquest.presentation.auth.RegisterScreen
import com.example.afyaquest.presentation.auth.SplashScreen
import com.example.afyaquest.presentation.dashboard.DashboardScreen
import com.example.afyaquest.presentation.dailyquestions.DailyQuestionsScreen
import com.example.afyaquest.presentation.map.MapScreen
import com.example.afyaquest.presentation.report.DailyReportScreen
import com.example.afyaquest.presentation.videomodules.VideoModulesScreen
import com.example.afyaquest.presentation.videomodules.VideoModulesViewModel
import com.example.afyaquest.presentation.lessons.LessonsScreen
import com.example.afyaquest.presentation.chat.ChatScreen
import com.example.afyaquest.presentation.profile.ProfileScreen
import com.example.afyaquest.presentation.modulequiz.ModuleQuizScreen
import com.example.afyaquest.presentation.settings.SettingsScreen
import com.example.afyaquest.presentation.videoplayer.VideoPlayerScreen

/**
 * Navigation graph for the app.
 */
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash screen
        composable(route = Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        // Login screen
        composable(route = Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        // Register screen
        composable(route = Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        // Dashboard screen
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }

        // Daily Questions screen
        composable(route = Screen.DailyQuestions.route) {
            DailyQuestionsScreen(navController = navController)
        }

        // Map/Itinerary screen
        composable(route = Screen.Map.route) {
            MapScreen(navController = navController)
        }

        // Daily Report screen
        composable(route = Screen.DailyReport.route) {
            DailyReportScreen(navController = navController)
        }

        // Video Modules screen
        composable(route = Screen.VideoModules.route) {
            VideoModulesScreen(navController = navController)
        }

        // Interactive Lessons screen
        composable(route = Screen.Lessons.route) {
            LessonsScreen(navController = navController)
        }

        // AI Chat screen
        composable(route = Screen.Chat.route) {
            ChatScreen(navController = navController)
        }

        // Profile screen
        composable(route = Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }

        // Settings screen
        composable(route = Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }

        // Module Quiz screen
        composable(route = Screen.ModuleQuiz.route) {
            ModuleQuizScreen(navController = navController)
        }

        // Video Player screen — shares the same VideoModulesViewModel instance as VideoModulesScreen
        composable(route = Screen.VideoPlayer.route) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getString("moduleId") ?: ""
            val videoModulesEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.VideoModules.route)
            }
            val viewModel: VideoModulesViewModel = hiltViewModel(videoModulesEntry)
            VideoPlayerScreen(moduleId = moduleId, navController = navController, viewModel = viewModel)
        }
    }
}
