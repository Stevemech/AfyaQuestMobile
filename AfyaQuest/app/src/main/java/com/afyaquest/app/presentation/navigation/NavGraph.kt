package com.afyaquest.app.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.afyaquest.app.presentation.auth.SESSION_EXPIRED_KEY
import com.afyaquest.app.presentation.chat.ChatScreen
import com.afyaquest.app.presentation.dailyquestions.DailyQuestionsScreen
import com.afyaquest.app.presentation.dashboard.DashboardScreen
import com.afyaquest.app.presentation.emergencyguide.CaseHistoryScreen
import com.afyaquest.app.presentation.emergencyguide.EmergencyGuideScreen
import com.afyaquest.app.presentation.emergencyguide.TriageFlowScreen
import com.afyaquest.app.presentation.learn.LearnScreen
import com.afyaquest.app.presentation.learn.LearnTopicScreen
import com.afyaquest.app.presentation.lessons.LessonDetailRoute
import com.afyaquest.app.presentation.map.MapScreen
import com.afyaquest.app.presentation.modulequiz.ModuleQuizScreen
import com.afyaquest.app.presentation.profile.ProfileScreen
import com.afyaquest.app.presentation.report.DailyReportScreen
import com.afyaquest.app.presentation.settings.SettingsScreen
import com.afyaquest.app.presentation.videomodules.ModuleDetailScreen
import com.afyaquest.app.presentation.videoplayer.VideoPlayerScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext

/** Length of the cross-fade between screens. */
private const val NAV_FADE_MS = 160

/**
 * App navigation host.
 *
 * Top-level destinations (Home, Learn, Tasks, Me) show a persistent [AppBottomBar]; every other
 * screen is a child with a back arrow. The bar lives outside the NavHost so it never re-animates
 * between tabs, and the host consumes the navigation-bar inset while the bar is visible so the
 * inner screens' Scaffolds do not double-pad the bottom. The status-bar inset is always consumed
 * here: the host draws the status-bar strip itself.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    sessionExpired: Flow<Unit> = emptyFlow()
) {
    // The server ended the session (refresh token expired or revoked): back to Login, with every
    // signed-in screen removed from the back stack.
    LaunchedEffect(navController) {
        sessionExpired.collect {
            // The signal comes from a network thread; navigation must happen on the main thread
            // whatever dispatcher this effect happens to run on.
            withContext(Dispatchers.Main.immediate) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
                runCatching {
                    navController.getBackStackEntry(Screen.Login.route).savedStateHandle[SESSION_EXPIRED_KEY] = true
                }
            }
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute != null && currentRoute in topLevelRoutes

    // The background is painted here so the window never shows through while two screens
    // cross-fade.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Brand-coloured strip behind the status bar. The theme asks for a primary-coloured status
        // bar with light icons, but from Android 15 the window draws edge to edge and ignores
        // statusBarColor, which left white icons on white top bars. Drawing the strip here (and
        // consuming the inset so the screens do not pad for it again) restores that on every screen.
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.primary)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .consumeWindowInsets(WindowInsets.statusBars)
                .then(
                    if (showBottomBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars)
                    else Modifier
                )
        ) {
            // Navigation's default is a 700 ms cross-fade on every screen change, tab switches
            // included, with both screens drawn the whole time. That alone made the app feel
            // slow; a short fade reads as instant.
            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = { fadeIn(animationSpec = tween(NAV_FADE_MS)) },
                exitTransition = { fadeOut(animationSpec = tween(NAV_FADE_MS)) },
                popEnterTransition = { fadeIn(animationSpec = tween(NAV_FADE_MS)) },
                popExitTransition = { fadeOut(animationSpec = tween(NAV_FADE_MS)) }
            ) {
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

                composable(route = Screen.Topic.route) { entry ->
                    val topicId = entry.arguments?.getString(Screen.Topic.ARG_TOPIC_ID)
                    LearnTopicScreen(topicId = topicId, navController = navController)
                }

                composable(
                    route = Screen.VideoPlayer.route,
                    arguments = listOf(
                        navArgument(Screen.VideoPlayer.ARG_TOPIC) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { entry ->
                    val moduleId = entry.arguments?.getString("moduleId") ?: ""
                    val topicId = entry.arguments?.getString(Screen.VideoPlayer.ARG_TOPIC)
                    VideoPlayerScreen(
                        moduleId = moduleId,
                        navController = navController,
                        topicId = topicId
                    )
                }

                composable(route = Screen.ModuleQuiz.route) {
                    ModuleQuizScreen(navController = navController)
                }

                composable(route = Screen.LessonDetail.route) { entry ->
                    val lessonId = entry.arguments?.getString("lessonId") ?: ""
                    LessonDetailRoute(lessonId = lessonId, navController = navController)
                }

                // ---- Emergency Response Guide ----

                composable(route = Screen.EmergencyGuide.route) {
                    EmergencyGuideScreen(navController = navController)
                }

                composable(route = Screen.TriageFlow.route) {
                    TriageFlowScreen(navController = navController)
                }

                composable(route = Screen.CaseHistory.route) {
                    CaseHistoryScreen(navController = navController)
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
