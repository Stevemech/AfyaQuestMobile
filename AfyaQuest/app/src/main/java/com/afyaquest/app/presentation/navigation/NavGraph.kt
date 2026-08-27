package com.afyaquest.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.afyaquest.app.presentation.assignments.AssignmentsScreen
import com.afyaquest.app.presentation.auth.LoginScreen
import com.afyaquest.app.presentation.auth.RegisterScreen
import com.afyaquest.app.presentation.auth.SplashScreen
import com.afyaquest.app.presentation.chat.ChatScreen
import com.afyaquest.app.presentation.dailyquestions.DailyQuestionsScreen
import com.afyaquest.app.presentation.dashboard.DashboardScreen
import com.afyaquest.app.presentation.learn.LearnScreen
import com.afyaquest.app.presentation.lessons.LessonDetailRoute
import com.afyaquest.app.presentation.map.MapScreen
import com.afyaquest.app.presentation.modulequiz.ModuleQuizScreen
import com.afyaquest.app.presentation.profile.ProfileScreen
import com.afyaquest.app.presentation.report.DailyReportScreen
import com.afyaquest.app.presentation.settings.SettingsScreen
import com.afyaquest.app.presentation.videomodules.ModuleDetailScreen
import com.afyaquest.app.presentation.videoplayer.VideoPlayerScreen

/**
 * App navigation host.
 *
 * Top-level destinations (Home, Learn, Tasks, Me) show a persistent [AppBottomBar]; every other
 * screen is a child with a back arrow. The bar lives outside the NavHost so it never re-animates
 * between tabs, and the host consumes the navigation-bar inset while the bar is visible so the
 * inner screens' Scaffolds do not double-pad the bottom.
 */
@Composable
fun NavGraph(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute != null && currentRoute in topLevelRoutes

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (showBottomBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars)
                    else Modifier
                )
        ) {
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

                // ---- Top-level (bottom bar) destinations ----

                composable(route = Screen.Dashboard.route) {
                    DashboardScreen(navController = navController)
                }

                composable(
                    route = Screen.Learn.route,
                    arguments = listOf(
                        navArgument(Screen.Learn.ARG_TAB) {
                            type = NavType.StringType
                            defaultValue = Screen.Learn.TAB_VIDEOS
                        }
                    )
                ) { entry ->
                    val tab = entry.arguments?.getString(Screen.Learn.ARG_TAB) ?: Screen.Learn.TAB_VIDEOS
                    LearnScreen(navController = navController, initialTab = tab)
                }

                composable(route = Screen.Assignments.route) {
                    AssignmentsScreen(navController = navController)
                }

                composable(route = Screen.Profile.route) {
                    ProfileScreen(navController = navController)
                }

                // ---- Daily workflow ----

                composable(route = Screen.DailyQuestions.route) {
                    DailyQuestionsScreen(navController = navController)
                }

                composable(route = Screen.Map.route) {
                    MapScreen(navController = navController)
                }

                composable(route = Screen.DailyReport.route) {
                    DailyReportScreen(navController = navController)
                }

                // ---- Learning detail screens ----

                composable(route = Screen.ModuleDetail.route) { entry ->
                    val moduleNumber = entry.arguments?.getString("moduleNumber")?.toIntOrNull() ?: 1
                    ModuleDetailScreen(moduleNumber = moduleNumber, navController = navController)
                }

                composable(route = Screen.VideoPlayer.route) { entry ->
                    val moduleId = entry.arguments?.getString("moduleId") ?: ""
                    VideoPlayerScreen(moduleId = moduleId, navController = navController)
                }

                composable(route = Screen.ModuleQuiz.route) {
                    ModuleQuizScreen(navController = navController)
                }

                composable(route = Screen.LessonDetail.route) { entry ->
                    val lessonId = entry.arguments?.getString("lessonId") ?: ""
                    LessonDetailRoute(lessonId = lessonId, navController = navController)
                }

                // ---- Other ----

                composable(route = Screen.Chat.route) {
                    ChatScreen(navController = navController)
                }

                composable(route = Screen.Settings.route) {
                    SettingsScreen(navController = navController)
                }
            }
        }

        if (showBottomBar) {
            AppBottomBar(navController = navController, currentRoute = currentRoute)
        }
    }
}
