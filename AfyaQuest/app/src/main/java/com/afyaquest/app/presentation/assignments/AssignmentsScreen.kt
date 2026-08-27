package com.afyaquest.app.presentation.assignments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Warning
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
import com.afyaquest.app.presentation.components.DoneBadge
import com.afyaquest.app.presentation.components.EmptyState
import com.afyaquest.app.presentation.components.ErrorState
import com.afyaquest.app.presentation.components.HintRow
import com.afyaquest.app.presentation.components.LoadingState
import com.afyaquest.app.presentation.components.ProgressSummary
import com.afyaquest.app.presentation.navigation.Screen
import com.afyaquest.app.presentation.navigation.navigateSingle
import com.afyaquest.app.presentation.navigation.openLearn
import com.afyaquest.app.presentation.navigation.openLesson
import com.afyaquest.app.presentation.navigation.openVideo
import com.afyaquest.app.presentation.videomodules.VideoModulesViewModel
import com.afyaquest.app.ui.theme.AfyaSuccess
import com.afyaquest.app.ui.theme.AfyaWarning
import com.afyaquest.app.util.DateUtils
import com.afyaquest.app.util.Resource

/**
 * "Tasks" bottom tab: everything the admin assigned to this CHV.
 * Top-level destination, so no back arrow; the bottom bar handles navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsScreen(
    navController: NavController,
    viewModel: AssignmentsViewModel = hiltViewModel()
) {
    val assignmentsState by viewModel.assignmentsState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_tasks), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadAssignments() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (assignmentsState) {
                is Resource.Loading, null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingState()
                    }
                }
                is Resource.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorState(
                            message = stringResource(R.string.failed_to_load_assignments),
                            onRetry = { viewModel.loadAssignments() }
                        )
                    }
                }
                is Resource.Success -> {
                    if (uiState.all.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(
                                title = stringResource(R.string.no_assignments),
                                message = stringResource(R.string.no_assignments_desc),
                                icon = Icons.Outlined.Checklist,
                                actionText = stringResource(R.string.refresh),
                                onAction = { viewModel.loadAssignments() }
                            )
                        }
                    } else {
                        AssignmentStatsCard(
                            total = uiState.total,
                            mandatory = uiState.mandatory,
                            pending = uiState.pending,
                            completed = uiState.completed
                        )

                        FilterChips(
                            selectedFilter = selectedFilter,
                            onFilterSelected = { viewModel.setFilter(it) }
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.visible) { assignment ->
                                AssignmentCard(
                                    assignment = assignment,
                                    onOpen = openActionFor(assignment, navController)
                                )
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The one place that decides where a task opens. Returns null when there is nothing to open,
 * so the card shows a hint instead of a dead-end button. Never navigates back to this screen.
 */
private fun openActionFor(a: AssignmentDto, navController: NavController): (() -> Unit)? {
    val moduleAction: (() -> Unit)? = a.moduleId?.takeIf { it.isNotBlank() }?.let { moduleId ->
        {
            val n = VideoModulesViewModel.allVideos().find { it.id == moduleId }?.moduleNumber
            if (n != null) navController.openVideo(n, moduleId) else navController.openLearn(Screen.Learn.TAB_VIDEOS)
        }
    }
    val lessonAction: (() -> Unit)? = a.lessonId?.takeIf { it.isNotBlank() }?.let { lessonId ->
        { navController.openLesson(lessonId) }
    }
    return when (a.type) {
        "module", "video" -> moduleAction ?: { navController.openLearn(Screen.Learn.TAB_VIDEOS) }
        "lesson" -> lessonAction
        "report" -> {
            { navController.navigateSingle(Screen.DailyReport.route) }
        }
        else -> moduleAction ?: lessonAction
    }
}

@Composable
fun AssignmentStatsCard(
    total: Int,
    mandatory: Int,
    pending: Int,
    completed: Int = total - pending
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    value = total.toString(),
                    label = stringResource(R.string.total_assigned)
                )
                StatColumn(
                    value = mandatory.toString(),
                    label = stringResource(R.string.mandatory)
                )
                StatColumn(
                    value = pending.toString(),
                    label = stringResource(R.string.pending)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ProgressSummary(
                label = stringResource(R.string.completed),
                done = completed,
                total = total
            )
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun FilterChips(
    selectedFilter: AssignmentFilter,
    onFilterSelected: (AssignmentFilter) -> Unit
) {
    val filters = listOf(
        AssignmentFilter.ALL to stringResource(R.string.all),
        AssignmentFilter.MANDATORY to stringResource(R.string.mandatory),
        AssignmentFilter.MODULES to stringResource(R.string.modules),
        AssignmentFilter.LESSONS to stringResource(R.string.lessons),
        AssignmentFilter.REPORTS to stringResource(R.string.reports)
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { (filter, label) ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(label) }
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

/** Localized display name of the linked lesson / video, falling back to a humanized id. */
@Composable
private fun assignmentItemName(assignment: AssignmentDto): String? {
    val lessonId = assignment.lessonId
    val moduleId = assignment.moduleId
    return when {
        !lessonId.isNullOrBlank() -> when (lessonId) {
            "lesson-1" -> stringResource(R.string.lesson_1_title)
            "lesson-2" -> stringResource(R.string.lesson_2_title)
            "lesson-3" -> stringResource(R.string.lesson_3_title)
            "lesson-4" -> stringResource(R.string.lesson_4_title)
            "lesson-5" -> stringResource(R.string.lesson_5_title)
            "lesson-6" -> stringResource(R.string.lesson_6_title)
            else -> humanizeId(lessonId)
        }
        !moduleId.isNullOrBlank() ->
            VideoModulesViewModel.allVideos().find { it.id == moduleId }?.title ?: humanizeId(moduleId)
        else -> null
    }
}

private fun humanizeId(id: String): String =
    id.replace("-", " ").replaceFirstChar { it.uppercase() }

private data class TaskTypeVisual(val icon: ImageVector, val label: String)

@Composable
private fun taskTypeVisual(assignment: AssignmentDto): TaskTypeVisual = when (assignment.type) {
    "module", "video" -> TaskTypeVisual(Icons.Filled.PlayCircle, stringResource(R.string.video_modules))
    "lesson" -> TaskTypeVisual(Icons.AutoMirrored.Filled.MenuBook, stringResource(R.string.interactive_lessons))
    "report" -> TaskTypeVisual(Icons.Filled.Description, stringResource(R.string.daily_report))
    else -> TaskTypeVisual(Icons.Filled.Checklist, stringResource(R.string.field_tasks_type_generic))
}

@Composable
fun AssignmentCard(
    assignment: AssignmentDto,
    onOpen: (() -> Unit)?
) {
    val isCompleted = assignment.status == "completed"
    val isOverdue = !isCompleted && DateUtils.isBeforeToday(assignment.dueDate)
    val visual = taskTypeVisual(assignment)
    val itemName = assignmentItemName(assignment)

    val statusColor = when (assignment.status) {
        "completed" -> AfyaSuccess
        "in_progress" -> AfyaWarning
        else -> MaterialTheme.colorScheme.primary
    }
    val statusLabel = when (assignment.status) {
        "completed" -> stringResource(R.string.completed)
        "in_progress" -> stringResource(R.string.in_progress)
        else -> stringResource(R.string.assigned)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: type icon + type + item name + mandatory badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = visual.icon,
                            contentDescription = visual.label,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = visual.label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (itemName != null) {
                            Text(
                                text = itemName,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (assignment.mandatory) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text(
                            text = stringResource(R.string.mandatory),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status + due date row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCompleted) {
                        DoneBadge(size = 20.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                    }
                }

                val dueDate = assignment.dueDate
                if (dueDate != null) {
                    DueDateText(dueDate = dueDate, isOverdue = isOverdue)
                }
            }

            // Assigned date
            if (assignment.assignedAt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.assigned_on, DateUtils.formatLocalized(assignment.assignedAt)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                onOpen == null -> HintRow(text = stringResource(R.string.field_tasks_no_link_hint))
                isCompleted -> {
                    TextButton(
                        onClick = onOpen,
                        modifier = Modifier
                            .align(Alignment.End)
                            .heightIn(min = 44.dp)
                    ) {
                        Text(stringResource(R.string.field_tasks_review))
                    }
                }
                else -> {
                    Button(
                        onClick = onOpen,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                    ) {
                        Text(text = openButtonLabel(assignment))
                    }
                }
            }
        }
    }
}

@Composable
private fun openButtonLabel(assignment: AssignmentDto): String = when (assignment.type) {
    "module", "video" -> stringResource(R.string.field_tasks_open_video)
    "lesson" -> stringResource(R.string.field_tasks_open_lesson)
    "report" -> stringResource(R.string.field_tasks_open_report)
    else -> when {
        !assignment.moduleId.isNullOrBlank() -> stringResource(R.string.field_tasks_open_video)
        !assignment.lessonId.isNullOrBlank() -> stringResource(R.string.field_tasks_open_lesson)
        else -> stringResource(R.string.open)
    }
}

@Composable
private fun DueDateText(dueDate: String, isOverdue: Boolean) {
    val formatted = DateUtils.formatLocalized(dueDate)
    if (isOverdue) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = stringResource(R.string.field_tasks_overdue),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.due_date_label, formatted),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(R.string.field_tasks_overdue),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    } else {
        Text(
            text = stringResource(R.string.due_date_label, formatted),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
