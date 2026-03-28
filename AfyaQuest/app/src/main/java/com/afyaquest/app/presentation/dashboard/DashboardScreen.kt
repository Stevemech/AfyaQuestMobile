package com.afyaquest.app.presentation.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.afyaquest.app.R
import com.afyaquest.app.presentation.profile.ProfileViewModel
import com.afyaquest.app.util.LanguageManager
import com.afyaquest.app.data.remote.dto.AssignmentDto
import com.afyaquest.app.presentation.assignments.AssignmentsViewModel
import com.afyaquest.app.presentation.auth.AuthViewModel
import com.afyaquest.app.presentation.navigation.Screen
import com.afyaquest.app.presentation.components.SyncStatusIndicator
import com.afyaquest.app.util.Resource
import kotlinx.coroutines.launch

/**
 * Dashboard screen - main hub of the app
 * Displays user stats, daily tasks, and learning center
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    assignmentsViewModel: AssignmentsViewModel = hiltViewModel()
) {
    val xpData by dashboardViewModel.xpData.collectAsState()
    val assignmentsState by assignmentsViewModel.assignmentsState.collectAsState()
    val scrollState = rememberScrollState()

    // Re-fetch assignments every time the dashboard is shown
    LaunchedEffect(Unit) {
        assignmentsViewModel.loadAssignments()
    }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showClockDialog by remember { mutableStateOf(false) }
    val isConnected by dashboardViewModel.isConnected.collectAsState()
    val unsyncedCount by dashboardViewModel.unsyncedCount.collectAsState()
    val isSyncing by dashboardViewModel.isSyncing.collectAsState()
    val syncError by dashboardViewModel.syncError.collectAsState()
    val isClockActive by dashboardViewModel.isClockActive.collectAsState()
    val clockLoading by dashboardViewModel.clockLoading.collectAsState()
    val clockError by dashboardViewModel.clockError.collectAsState()
    var showLanguageMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    // Language toggle
                    Box {
                        IconButton(onClick = { showLanguageMenu = true }) {
                            Icon(Icons.Default.Language, contentDescription = stringResource(R.string.language))
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.english)) },
                                onClick = {
                                    showLanguageMenu = false
                                    scope.launch {
                                        profileViewModel.changeLanguage(LanguageManager.LANGUAGE_ENGLISH)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.espanol)) },
                                onClick = {
                                    showLanguageMenu = false
                                    scope.launch {
                                        profileViewModel.changeLanguage(LanguageManager.LANGUAGE_SPANISH)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.kaqchikel)) },
                                onClick = {
                                    showLanguageMenu = false
                                    scope.launch {
                                        profileViewModel.changeLanguage(LanguageManager.LANGUAGE_KAQCHIKEL)
                                    }
                                }
                            )
                        }
                    }
                    IconButton(onClick = {
                        navController.navigate(Screen.Profile.route)
                    }) {
                        Icon(Icons.Default.Person, contentDescription = stringResource(R.string.profile))
                    }
                    IconButton(onClick = {
                        navController.navigate(Screen.Settings.route)
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.logout))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Sync Status Indicator
            SyncStatusIndicator(
                isConnected = isConnected,
                unsyncedCount = unsyncedCount,
                isSyncing = isSyncing,
                errorMessage = syncError,
                onSyncClick = { dashboardViewModel.triggerSync() }
            )

            // Clock In/Out Status Banner
            ClockStatusBanner(
                isActive = isClockActive,
                isLoading = clockLoading,
                onToggle = { showClockDialog = true }
            )

            // Clock error snackbar
            if (clockError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = clockError ?: "",
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = { dashboardViewModel.dismissClockError() }) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                }
            }

            // Stats Header
            StatsHeader(
                streak = xpData.streak,
                xp = xpData.totalXP,
                lives = xpData.lives
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User Level Card
            UserLevelCard(
                level = xpData.level,
                rank = xpData.rank,
                progress = dashboardViewModel.getLevelProgress(),
                xpForNextLevel = dashboardViewModel.getXPForNextLevel()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Daily To-Do Section
            DailyTasksSection(navController)

            Spacer(modifier = Modifier.height(24.dp))

            // Assigned to You Section
            AssignedToYouSection(
                assignmentsState = assignmentsState,
                assignmentsViewModel = assignmentsViewModel,
                navController = navController
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Learning Center Section
            LearningCenterSection(navController)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Clock in/out confirmation dialog
    if (showClockDialog) {
        AlertDialog(
            onDismissRequest = { showClockDialog = false },
            title = {
                Text(
                    if (isClockActive) stringResource(R.string.clock_out)
                    else stringResource(R.string.clock_in)
                )
            },
            text = {
                Text(
                    if (isClockActive) stringResource(R.string.clock_out_confirm)
                    else stringResource(R.string.clock_in_confirm)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClockDialog = false
                    dashboardViewModel.toggleClockStatus()
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClockDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout)) },
            text = { Text(stringResource(R.string.logout_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Text(stringResource(R.string.logout), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun StatsHeader(
    streak: Int,
    xp: Int,
    lives: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Streak
        StatItem(
            icon = "🔥",
            value = streak.toString(),
            label = stringResource(R.string.streak)
        )

        // XP
        StatItem(
            icon = "💎",
            value = stringResource(R.string.xp_value_format, xp),
            label = stringResource(R.string.total_xp)
        )

        // Lives
        StatItem(
            icon = "❤️",
            value = lives.toString(),
            label = stringResource(R.string.lives)
        )
    }
}

@Composable
fun StatItem(
    icon: String,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 32.sp
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun translatedRank(rank: String): String {
    return when (rank) {
        "Beginner" -> stringResource(R.string.rank_beginner)
        "Novice" -> stringResource(R.string.rank_novice)
        "Apprentice" -> stringResource(R.string.rank_apprentice)
        "Practitioner" -> stringResource(R.string.rank_practitioner)
        "Expert" -> stringResource(R.string.rank_expert)
        "Master" -> stringResource(R.string.rank_master)
        "Grand Master" -> stringResource(R.string.rank_grand_master)
        else -> rank
    }
}

@Composable
fun UserLevelCard(
    level: Int,
    rank: String,
    progress: Float,
    xpForNextLevel: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.level_value, level),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = translatedRank(rank),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Level badge
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = level.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Column {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.xp_to_level_format, xpForNextLevel, level + 1),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DailyTasksSection(navController: NavController) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.daily_todo),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Daily Questions Task
        TaskCard(
            icon = "📊",
            title = stringResource(R.string.daily_questions),
            description = stringResource(R.string.daily_questions_desc),
            isRequired = true,
            onClick = {
                navController.navigate(Screen.DailyQuestions.route)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Daily Itinerary Task
        TaskCard(
            icon = "🗺️",
            title = stringResource(R.string.daily_itinerary),
            description = stringResource(R.string.daily_itinerary_desc),
            isRequired = true,
            onClick = {
                navController.navigate(Screen.Map.route)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Daily Report Task
        TaskCard(
            icon = "📝",
            title = stringResource(R.string.daily_report),
            description = stringResource(R.string.daily_report_desc),
            isRequired = true,
            onClick = {
                navController.navigate(Screen.DailyReport.route)
            }
        )
    }
}

@Composable
fun LearningCenterSection(navController: NavController) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.learning_center),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Video Modules Task
        TaskCard(
            icon = "🎬",
            title = stringResource(R.string.video_modules),
            description = stringResource(R.string.video_modules_desc),
            isRequired = false,
            onClick = {
                navController.navigate(Screen.VideoModules.route)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Lessons Task
        TaskCard(
            icon = "📚",
            title = stringResource(R.string.interactive_lessons),
            description = stringResource(R.string.interactive_lessons_desc),
            isRequired = false,
            onClick = {
                navController.navigate(Screen.Lessons.route)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // AI Chat Assistant
        TaskCard(
            icon = "💬",
            title = stringResource(R.string.chat_with_steve),
            description = stringResource(R.string.chat_with_steve_desc),
            isRequired = false,
            onClick = {
                navController.navigate(Screen.Chat.route)
            }
        )
    }
}

@Composable
fun AssignedToYouSection(
    assignmentsState: Resource<List<AssignmentDto>>?,
    assignmentsViewModel: AssignmentsViewModel,
    navController: NavController
) {
    // Use merged assignments (local completion + API status)
    val allMerged = if (assignmentsState is Resource.Success) {
        assignmentsViewModel.getFilteredAssignments()
    } else {
        emptyList()
    }
    val pending = allMerged.filter { it.status != "completed" }
    if (pending.isEmpty() && assignmentsState !is Resource.Loading) return

    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.assigned_to_you),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (pending.isNotEmpty()) {
                TextButton(onClick = { navController.navigate(Screen.Assignments.route) }) {
                    Text(stringResource(R.string.view_all))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (assignmentsState) {
            is Resource.Loading, null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
            is Resource.Error -> {
                // Silently hide on error — assignments are supplementary
            }
            is Resource.Success -> {
                // Show up to 3 pending assignments as compact cards
                pending.take(3).forEach { assignment ->
                    AssignmentPreviewCard(
                        assignment = assignment,
                        onClick = {
                            when (assignment.type) {
                                "module", "video" -> navController.navigate(Screen.VideoModules.route)
                                "lesson" -> navController.navigate(Screen.Lessons.route)
                                "report" -> navController.navigate(Screen.DailyReport.route)
                                else -> when {
                                    !assignment.moduleId.isNullOrBlank() ->
                                        navController.navigate(Screen.VideoModules.route)
                                    !assignment.lessonId.isNullOrBlank() ->
                                        navController.navigate(Screen.Lessons.route)
                                    else ->
                                        navController.navigate(Screen.Assignments.route)
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (pending.size > 3) {
                    TextButton(
                        onClick = { navController.navigate(Screen.Assignments.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.more_assignments, pending.size - 3))
                    }
                }
            }
        }
    }
}

@Composable
fun AssignmentPreviewCard(
    assignment: AssignmentDto,
    onClick: () -> Unit
) {
    val icon = when (assignment.type) {
        "module", "video" -> "🎬"
        "lesson" -> "📚"
        "report" -> "📝"
        else -> "📋"
    }

    val typeLabel = when (assignment.type) {
        "module", "video" -> stringResource(R.string.video_modules)
        "lesson" -> stringResource(R.string.interactive_lessons)
        "report" -> stringResource(R.string.daily_report)
        else -> assignment.type ?: "Task"
    }

    val rawId = assignment.moduleId ?: assignment.lessonId
    val itemName = getAssignmentDisplayName(rawId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (assignment.mandatory)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = typeLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (assignment.mandatory) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.mandatory),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (itemName != null) {
                    Text(
                        text = itemName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (assignment.dueDate != null) {
                Text(
                    text = formatDueDate(assignment.dueDate),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Maps admin module/lesson IDs to human-readable display names.
 * Returns the raw ID if no mapping is found.
 */
private fun getAssignmentDisplayName(id: String?): String? {
    if (id == null) return null
    val videoNames = com.afyaquest.app.presentation.videomodules.VideoModulesViewModel.allVideos()
        .associate { it.id to it.title }
    val lessonNames = mapOf(
        "lesson-1" to "Handwashing Techniques",
        "lesson-2" to "Balanced Diet for Children",
        "lesson-3" to "Prenatal Care Essentials",
        "lesson-4" to "Child Vaccination Schedule",
        "lesson-5" to "Malaria Prevention",
        "lesson-6" to "CPR Basics"
    )
    val names = videoNames + lessonNames
    return names[id] ?: id.replace("-", " ").replaceFirstChar { it.uppercase() }
}

private fun formatDueDate(isoDate: String): String {
    return try {
        val parts = isoDate.take(10).split("-")
        if (parts.size == 3) {
            val months = listOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            val month = months.getOrElse(parts[1].toInt() - 1) { parts[1] }
            val day = parts[2].toInt()
            "Due $month $day"
        } else {
            isoDate.take(10)
        }
    } catch (e: Exception) {
        isoDate.take(10)
    }
}

@Composable
fun TaskCard(
    icon: String,
    title: String,
    description: String,
    isRequired: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isRequired) {
                        Text(
                            text = " *",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ClockStatusBanner(
    isActive: Boolean,
    isLoading: Boolean,
    onToggle: () -> Unit
) {
    val activeGreen = Color(0xFF438894)
    val inactiveGray = Color(0xFF94A3B8)
    val bgColor by animateColorAsState(
        targetValue = if (isActive) activeGreen.copy(alpha = 0.1f) else inactiveGray.copy(alpha = 0.08f),
        label = "clockBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isActive) activeGreen else inactiveGray,
        label = "clockBorder"
    )
    val dotColor by animateColorAsState(
        targetValue = if (isActive) activeGreen else inactiveGray,
        label = "clockDot"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable(enabled = !isLoading) { onToggle() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isActive) stringResource(R.string.status_active)
                else stringResource(R.string.status_inactive),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) activeGreen else inactiveGray
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) inactiveGray else activeGreen
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isActive) stringResource(R.string.clock_out)
                    else stringResource(R.string.clock_in),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
