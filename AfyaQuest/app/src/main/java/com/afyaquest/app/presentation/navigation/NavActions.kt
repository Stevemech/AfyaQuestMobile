package com.afyaquest.app.presentation.navigation

import androidx.navigation.NavController

/**
 * Navigation helpers shared by every screen so the app has ONE set of navigation semantics.
 *
 * - Bottom-bar (top-level) destinations are switched with [navigateTopLevel]: the back stack is
 *   trimmed to the Dashboard, state is saved/restored, and no duplicates are created.
 * - Child screens are pushed with [navigateSingle] so a double tap never stacks two copies.
 * - Deep links to a specific learning item ([openModule], [openVideo], [openLesson]) build the
 *   proper stack (Learn -> detail) so system back always lands somewhere sensible.
 */

/** Routes that show the bottom navigation bar. Must match the destinations in [AppBottomBar]. */
val topLevelRoutes: List<String> = listOf(
    Screen.Dashboard.route,
    Screen.Learn.route,
    Screen.Assignments.route,
    Screen.Profile.route
)

/** Switch to a top-level (bottom bar) destination. */
fun NavController.navigateTopLevel(route: String, restoreState: Boolean = true) {
    navigate(route) {
        popUpTo(Screen.Dashboard.route) { saveState = true }
        launchSingleTop = true
        this.restoreState = restoreState
    }
}

/** Push a child destination without ever stacking a duplicate on a double tap. */
fun NavController.navigateSingle(route: String) {
    navigate(route) { launchSingleTop = true }
}

/** Pop everything above the Dashboard (used by "Back to Home" actions). */
fun NavController.goHome() {
    val popped = popBackStack(Screen.Dashboard.route, inclusive = false)
    if (!popped) navigateTopLevel(Screen.Dashboard.route)
}

/** Open the Learn hub on a specific tab (does not restore a previously selected tab). */
fun NavController.openLearn(tab: String = Screen.Learn.TAB_VIDEOS) {
    navigateTopLevel(Screen.Learn.createRoute(tab), restoreState = false)
}

/** Open a specific module folder: Learn(videos) -> ModuleDetail. */
fun NavController.openModule(moduleNumber: Int) {
    openLearn(Screen.Learn.TAB_VIDEOS)
    navigateSingle(Screen.ModuleDetail.createRoute(moduleNumber))
}

/** Open a specific video: Learn(videos) -> ModuleDetail -> VideoPlayer. */
fun NavController.openVideo(moduleNumber: Int, videoId: String) {
    openModule(moduleNumber)
    navigateSingle(Screen.VideoPlayer.createRoute(videoId))
}

/** Open a specific lesson: Learn(lessons) -> LessonDetail. */
fun NavController.openLesson(lessonId: String) {
    openLearn(Screen.Learn.TAB_LESSONS)
    navigateSingle(Screen.LessonDetail.createRoute(lessonId))
}
