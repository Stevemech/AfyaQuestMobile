package com.afyaquest.app.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import com.afyaquest.app.R

/** One tab of the persistent bottom navigation bar. */
data class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    /** Route actually navigated to (may carry default args). */
    val navigateRoute: String = route
)

val topLevelDestinations: List<TopLevelDestination> = listOf(
    TopLevelDestination(
        route = Screen.Dashboard.route,
        labelRes = R.string.nav_home,
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home
    ),
    TopLevelDestination(
        route = Screen.Learn.route,
        labelRes = R.string.nav_learn,
        icon = Icons.Outlined.School,
        selectedIcon = Icons.Filled.School,
        navigateRoute = Screen.Learn.createRoute(Screen.Learn.TAB_VIDEOS)
    ),
    TopLevelDestination(
        route = Screen.Assignments.route,
        labelRes = R.string.nav_tasks,
        icon = Icons.Outlined.Checklist,
        selectedIcon = Icons.Filled.Checklist
    ),
    TopLevelDestination(
        route = Screen.Profile.route,
        labelRes = R.string.nav_me,
        icon = Icons.Outlined.Person,
        selectedIcon = Icons.Filled.Person
    )
)

/**
 * Persistent bottom navigation shown on top-level destinations only.
 * [currentRoute] is the route *pattern* of the current destination (e.g. "learn?tab={tab}").
 */
@Composable
fun AppBottomBar(navController: NavController, currentRoute: String?) {
    NavigationBar {
        topLevelDestinations.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) navController.navigateTopLevel(destination.navigateRoute)
                },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.icon,
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}
