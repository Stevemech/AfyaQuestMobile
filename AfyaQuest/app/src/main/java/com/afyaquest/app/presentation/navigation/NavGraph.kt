package com.afyaquest.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.afyaquest.app.presentation.auth.LoginScreen
import com.afyaquest.app.presentation.auth.RegisterScreen
import com.afyaquest.app.presentation.auth.SplashScreen
import com.afyaquest.app.presentation.dashboard.DashboardScreen
import com.afyaquest.app.presentation.dailyquestions.DailyQuestionsScreen
import com.afyaquest.app.presentation.map.MapScreen
import com.afyaquest.app.presentation.report.DailyReportScreen
import com.afyaquest.app.presentation.videomodules.VideoModulesScreen
import com.afyaquest.app.presentation.videomodules.VideoModulesViewModel
import com.afyaquest.app.presentation.videomodules.ModuleDetailScreen
import com.afyaquest.app.presentation.lessons.LessonsScreen
import com.afyaquest.app.presentation.chat.ChatScreen
import com.afyaquest.app.presentation.profile.ProfileScreen
import com.afyaquest.app.presentation.modulequiz.ModuleQuizScreen
import com.afyaquest.app.presentation.assignments.AssignmentsScreen
import com.afyaquest.app.presentation.settings.SettingsScreen
import com.afyaquest.app.presentation.videoplayer.VideoPlayerScreen

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
