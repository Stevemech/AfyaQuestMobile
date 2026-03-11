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
import com.example.afyaquest.presentation.videomodules.ModuleDetailScreen
import com.example.afyaquest.presentation.lessons.LessonsScreen
import com.example.afyaquest.presentation.chat.ChatScreen
import com.example.afyaquest.presentation.profile.ProfileScreen
import com.example.afyaquest.presentation.modulequiz.ModuleQuizScreen
import com.example.afyaquest.presentation.assignments.AssignmentsScreen
import com.example.afyaquest.presentation.settings.SettingsScreen
import com.example.afyaquest.presentation.videoplayer.VideoPlayerScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(route = Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        composable(route = Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        composable(route = Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        composable(route = Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }

        composable(route = Screen.DailyQuestions.route) {
            DailyQuestionsScreen(navController = navController)
        }

        composable(route = Screen.Map.route) {
            MapScreen(navController = navController)
        }

        composable(route = Screen.DailyReport.route) {
            DailyReportScreen(navController = navController)
        }

        composable(route = Screen.VideoModules.route) {
            VideoModulesScreen(navController = navController)
        }

        composable(route = Screen.ModuleDetail.route) { backStackEntry ->
            val moduleNumber = backStackEntry.arguments?.getString("moduleNumber")?.toIntOrNull() ?: 1
            val videoModulesEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.VideoModules.route)
            }
            val viewModel: VideoModulesViewModel = hiltViewModel(videoModulesEntry)
            ModuleDetailScreen(
                moduleNumber = moduleNumber,
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(route = Screen.Lessons.route) {
            LessonsScreen(navController = navController)
        }

        composable(route = Screen.Chat.route) {
            ChatScreen(navController = navController)
        }

        composable(route = Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }

        composable(route = Screen.Assignments.route) {
            AssignmentsScreen(navController = navController)
        }

        composable(route = Screen.ModuleQuiz.route) {
            ModuleQuizScreen(navController = navController)
        }

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
