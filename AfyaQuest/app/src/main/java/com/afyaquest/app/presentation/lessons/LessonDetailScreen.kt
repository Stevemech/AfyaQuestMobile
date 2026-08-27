package com.afyaquest.app.presentation.lessons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.afyaquest.app.R
import com.afyaquest.app.domain.model.Lesson
import com.afyaquest.app.presentation.components.CompletionDialog
import com.afyaquest.app.presentation.components.DoneBadge
import com.afyaquest.app.presentation.components.EmptyState
import com.afyaquest.app.presentation.components.HintRow
import com.afyaquest.app.presentation.components.LoadingState
import com.afyaquest.app.presentation.navigation.Screen
import com.afyaquest.app.ui.theme.AfyaSuccess

/**
 * Lesson detail as its own navigation destination (route [Screen.LessonDetail]).
 *
 * System back / the app bar arrow return to the lessons list. A thin reading-progress bar sits
 * under the app bar, the "Mark as complete" action is pinned at the bottom, and completion is
 * celebrated in a dialog that offers the next open lesson as the primary action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailRoute(
    lessonId: String,
    navController: NavController,
    viewModel: LessonsViewModel = hiltViewModel()
) {
    val lessons by viewModel.lessons.collectAsState()
    val completedIds by viewModel.completedLessons.collectAsState()
    val lesson = lessons.find { it.id == lessonId }
        ?.let { it.copy(completed = completedIds.contains(it.id)) }

    when {
        lessons.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingState()
            }
        }
        lesson == null -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.lesson), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = stringResource(R.string.lesson_ui_not_found),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        actionText = stringResource(R.string.lesson_ui_back_to_lessons),
                        onAction = { navController.popBackStack() }
                    )
                }
            }
        }
        else -> {
            LessonDetailContent(
                lesson = lesson,
                onBack = { navController.popBackStack() },
                onComplete = { viewModel.completeLesson(lesson.id) },
                nextLessonProvider = { viewModel.nextIncompleteLesson(afterId = lesson.id) },
                onOpenNext = { next ->
                    // Replace this lesson with the next one so back still returns to the list.
                    navController.navigate(Screen.LessonDetail.createRoute(next.id)) {
                        popUpTo(Screen.LessonDetail.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonDetailContent(
    lesson: Lesson,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    nextLessonProvider: () -> Lesson?,
    onOpenNext: (Lesson) -> Unit
) {
    val scrollState = rememberScrollState()
    var showCompletion by rememberSaveable { mutableStateOf(false) }
    var justCompleted by rememberSaveable { mutableStateOf(false) }
    val isCompleted = lesson.completed || justCompleted

    // Reading progress: 0 until laid out, 1 when the whole lesson fits on screen.
    val readFraction = when (scrollState.maxValue) {
        0 -> 1f
        Int.MAX_VALUE -> 0f
        else -> (scrollState.value.toFloat() / scrollState.maxValue).coerceIn(0f, 1f)
    }
    val readingProgressLabel = stringResource(R.string.lesson_ui_reading_progress)

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = lesson.title,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                )
                LinearProgressIndicator(
                    progress = { readFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .semantics { contentDescription = readingProgressLabel },
                    color = if (isCompleted) AfyaSuccess else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        },
        bottomBar = {
            if (!isCompleted) {
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            onComplete()
                            justCompleted = true
                            showCompletion = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .heightIn(min = 52.dp)
                    ) {
                        Text(stringResource(R.string.mark_complete_xp, lesson.points))
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            if (isCompleted) {
                CompletedCard()
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                HintRow(text = stringResource(R.string.lesson_ui_read_hint))
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Lesson title
            Text(
                text = lesson.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge(containerColor = difficultyColor(lesson.difficulty)) {
                    Text(
                        text = difficultyLabel(lesson.difficulty),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White
                    )
                }

                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        text = stringResource(R.string.min_format, lesson.estimatedMinutes.toString()),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(
                        text = stringResource(R.string.xp_format, lesson.points),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lesson content
            Text(
                text = lesson.content,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCompletion) {
        val next = nextLessonProvider()
        val backToLessons = stringResource(R.string.lesson_ui_back_to_lessons)
        if (next != null) {
            CompletionDialog(
                title = stringResource(R.string.lesson_completed),
                message = stringResource(R.string.lesson_ui_completed_message),
                primaryText = stringResource(R.string.lesson_ui_next_lesson),
                onPrimary = {
                    showCompletion = false
                    onOpenNext(next)
                },
                xpEarned = lesson.points,
                secondaryText = backToLessons,
                onSecondary = {
                    showCompletion = false
                    onBack()
                },
                onDismiss = { showCompletion = false }
            )
        } else {
            CompletionDialog(
                title = stringResource(R.string.lesson_completed),
                message = stringResource(R.string.lesson_ui_all_done_title),
                primaryText = backToLessons,
                onPrimary = {
                    showCompletion = false
                    onBack()
                },
                xpEarned = lesson.points,
                onDismiss = { showCompletion = false }
            )
        }
    }
}

@Composable
private fun CompletedCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AfyaSuccess.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DoneBadge(size = 32.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.lesson_completed),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
