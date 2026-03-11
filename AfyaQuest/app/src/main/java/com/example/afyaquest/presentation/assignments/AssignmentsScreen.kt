package com.example.afyaquest.presentation.assignments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.afyaquest.R
import com.example.afyaquest.data.remote.dto.AssignmentDto
import com.example.afyaquest.presentation.navigation.Screen
import com.example.afyaquest.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsScreen(
    navController: NavController,
    viewModel: AssignmentsViewModel = hiltViewModel()
) {
    val assignmentsState by viewModel.assignmentsState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    // Re-fetch every time the screen appears
    LaunchedEffect(Unit) {
        viewModel.loadAssignments()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.assignments), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is Resource.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.failed_to_load_assignments),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = (assignmentsState as Resource.Error).message ?: "",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadAssignments() }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
                is Resource.Success -> {
                    val allAssignments = (assignmentsState as Resource.Success).data ?: emptyList()

                    if (allAssignments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "📋", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.no_assignments),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.no_assignments_desc),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Stats summary
                        AssignmentStatsCard(
                            total = allAssignments.size,
                            mandatory = viewModel.getMandatoryCount(),
                            pending = viewModel.getPendingCount()
                        )

                        // Filter chips
                        FilterChips(
                            selectedFilter = selectedFilter,
                            onFilterSelected = { viewModel.setFilter(it) }
                        )

                        // Assignments list
                        val filtered = viewModel.getFilteredAssignments()
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered) { assignment ->
                                AssignmentCard(
                                    assignment = assignment,
                                    onNavigate = {
                                        when (assignment.type) {
                                            "module" -> navController.navigate(Screen.VideoModules.route)
                                            "lesson" -> navController.navigate(Screen.Lessons.route)
                                            "report" -> navController.navigate(Screen.DailyReport.route)
                                        }
                                    }
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

@Composable
fun AssignmentStatsCard(
    total: Int,
    mandatory: Int,
    pending: Int
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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

@Composable
fun AssignmentCard(
    assignment: AssignmentDto,
    onNavigate: () -> Unit
) {
    val (icon, typeLabel) = when (assignment.type) {
        "module" -> "🎬" to stringResource(R.string.video_modules)
        "lesson" -> "📚" to stringResource(R.string.interactive_lessons)
        "report" -> "📝" to stringResource(R.string.daily_report)
        else -> "📋" to (assignment.type ?: "Task")
    }

    val statusColor = when (assignment.status) {
        "completed" -> Color(0xFF438894)
        "in_progress" -> Color(0xFFEFA03F)
        else -> MaterialTheme.colorScheme.primary
    }

    val statusLabel = when (assignment.status) {
        "completed" -> stringResource(R.string.completed)
        "in_progress" -> stringResource(R.string.in_progress)
        else -> stringResource(R.string.assigned)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row: icon + type + mandatory badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = icon, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = typeLabel,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // Show module/lesson name
                        val itemId = assignment.moduleId ?: assignment.lessonId
                        if (itemId != null) {
                            Text(
                                text = getModuleDisplayName(itemId),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (assignment.mandatory) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
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
                // Status badge
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

                // Due date
                if (assignment.dueDate != null) {
                    Text(
                        text = stringResource(R.string.due_date_label, formatDate(assignment.dueDate)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Assigned date
            if (assignment.assignedAt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.assigned_on, formatDate(assignment.assignedAt)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action button for non-completed
            if (assignment.status != "completed") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onNavigate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (assignment.type) {
                            "module" -> stringResource(R.string.go_to_modules)
                            "lesson" -> stringResource(R.string.go_to_lessons)
                            "report" -> stringResource(R.string.go_to_report)
                            else -> stringResource(R.string.open)
                        }
                    )
                }
            }
        }
    }
}

private fun formatDate(isoDate: String): String {
    return try {
        // "2026-03-07T10:30:00.000Z" -> "Mar 7, 2026"
        val parts = isoDate.take(10).split("-")
        if (parts.size == 3) {
            val months = listOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            val month = months.getOrElse(parts[1].toInt() - 1) { parts[1] }
            val day = parts[2].toInt()
            val year = parts[0]
            "$month $day, $year"
        } else {
            isoDate.take(10)
        }
    } catch (e: Exception) {
        isoDate.take(10)
    }
}

private fun getModuleDisplayName(id: String): String {
    val videoNames = com.example.afyaquest.presentation.videomodules.VideoModulesViewModel.allVideos()
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
