package com.afyaquest.app.presentation.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.afyaquest.app.R
import com.afyaquest.app.data.remote.dto.AssignmentDto
import com.afyaquest.app.presentation.assignments.AssignmentsViewModel
import com.afyaquest.app.presentation.components.DoneBadge
import com.afyaquest.app.presentation.components.HintRow
import com.afyaquest.app.presentation.components.LoadingState
import com.afyaquest.app.presentation.components.ProgressSummary
import com.afyaquest.app.presentation.components.SyncStatusIndicator
import com.afyaquest.app.presentation.navigation.Screen
import com.afyaquest.app.presentation.navigation.navigateSingle
import com.afyaquest.app.presentation.navigation.navigateTopLevel
import com.afyaquest.app.presentation.navigation.openLearn
import com.afyaquest.app.presentation.navigation.openLesson
import com.afyaquest.app.presentation.navigation.openVideo
import com.afyaquest.app.presentation.profile.ProfileViewModel
import com.afyaquest.app.presentation.videomodules.VideoModulesViewModel
import com.afyaquest.app.ui.theme.AfyaSuccess
import com.afyaquest.app.util.DateUtils
import com.afyaquest.app.util.LanguageManager
import com.afyaquest.app.util.Resource
import kotlinx.coroutines.launch

/**
 * Dashboard screen - the Home hub.
 *
 * Answers "what do I still need to do today?" at a glance: daily to-do with done/undone state,
 * pending assignments that open the exact item, and learning progress. Profile and Logout are
 * reached via the bottom bar ("Me") and Settings, so the top bar only carries Language + Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    assignmentsViewModel: AssignmentsViewModel = hiltViewModel()
) {
    val xpData by dashboardViewModel.xpData.collectAsState()
    val dailyProgress by dashboardViewModel.dailyProgress.collectAsState()
    val learningProgress by dashboardViewModel.learningProgress.collectAsState()
    val assignmentsState by assignmentsViewModel.assignmentsState.collectAsState()
    val scrollState = rememberScrollState()

    // Re-fetch assignments every time the dashboard is shown
    LaunchedEffect(Unit) {
        assignmentsViewModel.loadAssignments()
    }
    var showClockDialog by remember { mutableStateOf(false) }
    val isConnected by dashboardViewModel.isConnected.collectAsState()
    val unsyncedCount by dashboardViewModel.unsyncedCount.collectAsState()
    val isSyncing by dashboardViewModel.isSyncing.collectAsState()
    val syncError by dashboardViewModel.syncError.collectAsState()
    val isClockActive by dashboardViewModel.isClockActive.collectAsState()
    val clockLoading by dashboardViewModel.clockLoading.collectAsState()
    val clockError by dashboardViewModel.clockError.collectAsState()

    Scaffold(
        topBar = {
            DashboardTopBar(
                onLanguageSelected = { profileViewModel.changeLanguage(it) },
                onSettingsClick = { navController.navigateSingle(Screen.Settings.route) }
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
            // Sync status (collapses to one line when everything is synced)
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

            // Clock error
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
            DailyTasksSection(navController = navController, progress = dailyProgress)

            Spacer(modifier = Modifier.height(24.dp))

            // Assigned to You Section
            AssignedToYouSection(
                assignmentsState = assignmentsState,
                assignmentsViewModel = assignmentsViewModel,
                navController = navController
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Learning Center Section
            LearningCenterSection(navController = navController, progress = learningProgress)

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
}

/** Top bar: title + Language menu + Settings. Home is a top-level tab, so there is no back arrow. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    onLanguageSelected: suspend (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    var showLanguageMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    TopAppBar(
        title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        actions = {
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
                            scope.launch { onLanguageSelected(LanguageManager.LANGUAGE_ENGLISH) }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.espanol)) },
                        onClick = {
                            showLanguageMenu = false
                            scope.launch { onLanguageSelected(LanguageManager.LANGUAGE_SPANISH) }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.kaqchikel)) },
                        onClick = {
                            showLanguageMenu = false
                            scope.launch { onLanguageSelected(LanguageManager.LANGUAGE_KAQCHIKEL) }
                        }
                    )
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.home_open_settings))
            }
        }
    )
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
        StatItem(
            icon = Icons.Default.LocalFireDepartment,
            value = streak.toString(),
            label = stringResource(R.string.streak)
        )
        StatItem(
            icon = Icons.Default.Stars,
            value = stringResource(R.string.xp_value_format, xp),
            label = stringResource(R.string.total_xp)
        )
        StatItem(
            icon = Icons.Default.Favorite,
            value = lives.toString(),
            label = stringResource(R.string.lives)
        )
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null, // the label below names the stat
            tint = Color.White,
            modifier = Modifier.size(32.dp)
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
        Column(modifier = Modifier.padding(16.dp)) {
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

            LinearProgressIndicator(
                progress = { (progress / 100f).coerceIn(0f, 1f) },
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

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun DailyTasksSection(
    navController: NavController,
    progress: DailyProgress
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle(stringResource(R.string.daily_todo))

        Spacer(modifier = Modifier.height(8.dp))

        ProgressSummary(
            label = stringResource(R.string.home_todo_progress_label),
            done = progress.doneCount,
            total = DailyProgress.TOTAL_TASKS
        )

        if (progress.allDone) {
            Spacer(modifier = Modifier.height(10.dp))
            AllDoneBanner()
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Daily Questions Task
        val correct = progress.questionsCorrect
        val totalQuestions = progress.questionsTotal
        val questionsStatus = if (progress.questionsDone && correct != null && totalQuestions != null) {
            stringResource(R.string.home_score_format, correct, totalQuestions)
        } else null
        TaskCard(
            icon = Icons.Default.Quiz,
            title = stringResource(R.string.daily_questions),
            description = stringResource(R.string.daily_questions_desc),
            isRequired = true,
            onClick = { navController.navigateSingle(Screen.DailyQuestions.route) },
            isDone = progress.questionsDone,
            statusText = questionsStatus
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Daily Itinerary Task
        val stopsStatus = if (progress.stopsTotal > 0) {
            stringResource(R.string.home_stops_visited_format, progress.stopsVisited, progress.stopsTotal)
        } else null
        TaskCard(
            icon = Icons.Default.Map,
            title = stringResource(R.string.daily_itinerary),
            description = stringResource(R.string.daily_itinerary_desc),
            isRequired = true,
            onClick = { navController.navigateSingle(Screen.Map.route) },
            isDone = progress.itineraryDone,
            statusText = stopsStatus
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Daily Report Task
        TaskCard(
            icon = Icons.Default.Description,
            title = stringResource(R.string.daily_report),
            description = stringResource(R.string.daily_report_desc),
            isRequired = true,
            onClick = { navController.navigateSingle(Screen.DailyReport.route) },
            isDone = progress.reportDone
        )
    }
}

/** One-line success banner shown when all three daily tasks are done. */
@Composable
private fun AllDoneBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = AfyaSuccess.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AfyaSuccess,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.all_done_today),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = AfyaSuccess
            )
        }
    }
}

@Composable
fun LearningCenterSection(
    navController: NavController,
    progress: LearningProgress
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle(stringResource(R.string.learning_center))

        Spacer(modifier = Modifier.height(12.dp))

        // Video Modules
        TaskCard(
            icon = Icons.Default.PlayCircle,
            title = stringResource(R.string.video_modules),
            description = stringResource(R.string.video_modules_desc),
            isRequired = false,
            onClick = { navController.openLearn(Screen.Learn.TAB_VIDEOS) },
            progress = progress.videosWatched to progress.videosTotal
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Lessons
        TaskCard(
            icon = Icons.Default.MenuBook,
            title = stringResource(R.string.interactive_lessons),
            description = stringResource(R.string.interactive_lessons_desc),
            isRequired = false,
            onClick = { navController.openLearn(Screen.Learn.TAB_LESSONS) },
            progress = progress.lessonsCompleted to progress.lessonsTotal
        )

        Spacer(modifier = Modifier.height(12.dp))

        // AI Chat Assistant
        TaskCard(
            icon = Icons.Default.Chat,
            title = stringResource(R.string.chat_with_steve),
            description = stringResource(R.string.chat_with_steve_desc),
            isRequired = false,
            onClick = { navController.navigateSingle(Screen.Chat.route) }
        )
    }
}

/** Open the exact assigned item rather than a list. */
private fun NavController.openAssignment(assignment: AssignmentDto) {
    fun openModuleOrVideos(moduleId: String?) {
        val n = VideoModulesViewModel.allVideos().find { it.id == moduleId }?.moduleNumber
        if (n != null && moduleId != null) openVideo(n, moduleId) else openLearn(Screen.Learn.TAB_VIDEOS)
    }
    fun openLessonOrLessons(lessonId: String?) {
        if (!lessonId.isNullOrBlank()) openLesson(lessonId) else openLearn(Screen.Learn.TAB_LESSONS)
    }
    when (assignment.type) {
        "module", "video" -> openModuleOrVideos(assignment.moduleId)
        "lesson" -> openLessonOrLessons(assignment.lessonId)
        "report" -> navigateSingle(Screen.DailyReport.route)
        else -> when {
            !assignment.moduleId.isNullOrBlank() -> openModuleOrVideos(assignment.moduleId)
            !assignment.lessonId.isNullOrBlank() -> openLessonOrLessons(assignment.lessonId)
            else -> navigateTopLevel(Screen.Assignments.route)
        }
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

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(stringResource(R.string.assigned_to_you))
            TextButton(onClick = { navController.navigateTopLevel(Screen.Assignments.route) }) {
                Text(stringResource(R.string.view_all))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (assignmentsState) {
            is Resource.Loading, null -> {
                LoadingState()
            }
            is Resource.Error -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.failed_to_load_assignments),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = { assignmentsViewModel.loadAssignments() }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            is Resource.Success -> {
                if (pending.isEmpty()) {
                    HintRow(
                        text = stringResource(R.string.home_no_assignments),
                        icon = Icons.Default.Checklist
                    )
                } else {
                    // Show up to 3 pending assignments as compact cards
                    pending.take(3).forEach { assignment ->
                        AssignmentPreviewCard(
                            assignment = assignment,
                            onClick = { navController.openAssignment(assignment) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (pending.size > 3) {
                        TextButton(
                            onClick = { navController.navigateTopLevel(Screen.Assignments.route) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.more_assignments, pending.size - 3))
                        }
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
        "module", "video" -> Icons.Default.PlayCircle
        "lesson" -> Icons.Default.MenuBook
        "report" -> Icons.Default.Description
        else -> Icons.Default.Checklist
    }

    val typeLabel = when (assignment.type) {
        "module", "video" -> stringResource(R.string.video_modules)
        "lesson" -> stringResource(R.string.interactive_lessons)
        "report" -> stringResource(R.string.daily_report)
        else -> assignment.type ?: stringResource(R.string.home_task_label)
    }

    val rawId = assignment.moduleId ?: assignment.lessonId
    val itemName = assignmentDisplayName(rawId)
    val isOverdue = DateUtils.isBeforeToday(assignment.dueDate)

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
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
                Icon(
                    imageVector = icon,
                    contentDescription = null, // type label is shown next to it
                    tint = if (assignment.mandatory)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
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

            if (!assignment.dueDate.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.home_due_format, DateUtils.formatLocalized(assignment.dueDate)),
                    fontSize = 11.sp,
                    fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                    color = if (isOverdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Human-readable, localized name for an assigned module/lesson id.
 * Falls back to a prettified version of the raw id.
 */
@Composable
private fun assignmentDisplayName(id: String?): String? {
    if (id.isNullOrBlank()) return null
    val lessonTitle = when (id) {
        "lesson-1" -> stringResource(R.string.lesson_1_title)
        "lesson-2" -> stringResource(R.string.lesson_2_title)
        "lesson-3" -> stringResource(R.string.lesson_3_title)
        "lesson-4" -> stringResource(R.string.lesson_4_title)
        "lesson-5" -> stringResource(R.string.lesson_5_title)
        "lesson-6" -> stringResource(R.string.lesson_6_title)
        else -> null
    }
    if (lessonTitle != null) return lessonTitle
    val videoTitle = remember(id) {
        VideoModulesViewModel.allVideos().find { it.id == id }?.title
    }
    return videoTitle ?: id.replace("-", " ").replaceFirstChar { it.uppercase() }
}

/**
 * Card used for daily tasks and learning-center entries.
 *
 * @param isDone      tints the card, overlays a [DoneBadge] on the icon and shows "Done today".
 * @param statusText  optional one-line status under the description (e.g. "Score 3/3").
 * @param progress    optional (done, total) pair rendered as a thin bar + "N/M".
 */
@Composable
fun TaskCard(
    icon: ImageVector,
    title: String,
    description: String,
    isRequired: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDone: Boolean = false,
    statusText: String? = null,
    progress: Pair<Int, Int>? = null
) {
    val containerColor = if (isDone) AfyaSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
    val iconBg = if (isDone) AfyaSuccess.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer
    val iconTint = if (isDone) AfyaSuccess else MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with optional done badge overlapping the corner
            Box(modifier = Modifier.size(60.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null, // title is right next to it
                        tint = iconTint,
                        modifier = Modifier.size(32.dp)
                    )
                }
                if (isDone) {
                    DoneBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isRequired && !isDone) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SmallPill(
                            text = stringResource(R.string.required_label),
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

                if (isDone) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AfyaSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.done_today),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AfyaSuccess
                        )
                        if (statusText != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (statusText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (progress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CardProgressLine(done = progress.first, total = progress.second)
                }
            }
        }
    }
}

/** Tiny outlined pill used for the "Required" marker. */
@Composable
private fun SmallPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/** Thin progress bar + "N/M" inside a card. */
@Composable
private fun CardProgressLine(done: Int, total: Int) {
    val safeTotal = total.coerceAtLeast(0)
    val fraction = if (safeTotal == 0) 0f else (done.toFloat() / safeTotal).coerceIn(0f, 1f)
    val complete = safeTotal > 0 && done >= safeTotal
    val barColor = if (complete) AfyaSuccess else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$done/$safeTotal",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = barColor
        )
    }
}

@Composable
fun ClockStatusBanner(
    isActive: Boolean,
    isLoading: Boolean,
    onToggle: () -> Unit
) {
    val activeGreen = AfyaSuccess
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

    // Only the button toggles; the banner itself is not clickable.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
            if (!isActive) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_clock_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.heightIn(min = 44.dp)
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
