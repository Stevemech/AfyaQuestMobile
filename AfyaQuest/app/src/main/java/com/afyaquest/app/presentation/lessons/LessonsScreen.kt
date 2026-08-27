package com.afyaquest.app.presentation.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.afyaquest.app.R
import com.afyaquest.app.domain.model.Difficulty
import com.afyaquest.app.domain.model.Lesson
import com.afyaquest.app.presentation.components.DoneBadge
import com.afyaquest.app.presentation.components.EmptyState
import com.afyaquest.app.presentation.components.NextStepCard
import com.afyaquest.app.presentation.components.ProgressSummary
import com.afyaquest.app.presentation.navigation.Screen
import com.afyaquest.app.presentation.navigation.navigateSingle
import com.afyaquest.app.ui.theme.AfyaError
import com.afyaquest.app.ui.theme.AfyaSuccess
import com.afyaquest.app.ui.theme.AfyaWarning

/**
 * Interactive Lessons list.
 *
 * Hosted inside the Learn hub's "Lessons" tab, so it renders content only (no Scaffold or
 * TopAppBar). Shows overall progress, the next lesson to continue with, category filters and
 * the numbered lesson cards. Tapping a lesson opens [LessonDetailRoute] as its own destination
 * so system back returns here instead of leaving the learning area.
 */
@Composable
fun LessonsContent(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: LessonsViewModel = hiltViewModel()
) {
    val lessons by viewModel.lessons.collectAsState()
    val completedIds by viewModel.completedLessons.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val allLessons = lessons.map { it.copy(completed = completedIds.contains(it.id)) }
    val numberById = allLessons.mapIndexed { index, lesson -> lesson.id to index + 1 }.toMap()
    val categories = allLessons.map { it.category }.distinct()
    val filteredLessons = if (selectedCategory == null) {
        allLessons
    } else {
        allLessons.filter { it.category == selectedCategory }
    }
    val nextLesson = allLessons.firstOrNull { !it.completed }
    val completedCount = allLessons.count { it.completed }

    val openLesson: (Lesson) -> Unit = { lesson ->
        navController.navigateSingle(Screen.LessonDetail.createRoute(lesson.id))
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "progress") {
            ProgressSummary(
                label = stringResource(R.string.lesson_ui_progress_label),
                done = completedCount,
                total = allLessons.size
            )
        }

        item(key = "next") {
            if (nextLesson != null) {
                NextStepCard(
                    title = nextLesson.title,
                    subtitle = stringResource(
                        R.string.lesson_ui_number_format,
                        numberById[nextLesson.id] ?: 0
                    ),
                    actionText = stringResource(R.string.continue_label),
                    onClick = { openLesson(nextLesson) },
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    eyebrow = stringResource(R.string.next_up)
                )
            } else if (allLessons.isNotEmpty()) {
                AllLessonsDoneCard()
            }
        }

        if (categories.isNotEmpty()) {
            item(key = "categories") {
                Column {
                    Text(
                        text = stringResource(R.string.categories),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { viewModel.setCategory(null) },
                                label = { Text(stringResource(R.string.all)) }
                            )
                        }
                        items(categories) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = {
                                    if (selectedCategory == category) viewModel.setCategory(null)
                                    else viewModel.setCategory(category)
                                },
                                label = { Text(viewModel.getCategoryDisplayName(category)) }
                            )
                        }
                    }
                }
            }
        }

        if (filteredLessons.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = stringResource(R.string.no_lessons_available),
                    icon = Icons.AutoMirrored.Filled.MenuBook
                )
            }
        } else {
            items(filteredLessons, key = { it.id }) { lesson ->
                LessonCard(
                    lesson = lesson,
                    onClick = { openLesson(lesson) },
                    number = numberById[lesson.id] ?: 0
                )
            }
        }
    }
}

@Composable
private fun AllLessonsDoneCard() {
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
            DoneBadge(size = 36.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.lesson_ui_all_done_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.lesson_ui_all_done_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Colour used for the difficulty badge of a lesson. */
internal fun difficultyColor(difficulty: Difficulty): Color = when (difficulty) {
    Difficulty.EASY -> AfyaSuccess
    Difficulty.MEDIUM -> AfyaWarning
    Difficulty.HARD -> AfyaError
}

@Composable
internal fun difficultyLabel(difficulty: Difficulty): String = when (difficulty) {
    Difficulty.EASY -> stringResource(R.string.difficulty_easy)
    Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium)
    Difficulty.HARD -> stringResource(R.string.difficulty_hard)
}

/**
 * One lesson in the list. [number] is the lesson's position (1..N) in the full list and is
 * shown in a circle on the left; it is replaced by a green check once the lesson is completed.
 */
@Composable
fun LessonCard(
    lesson: Lesson,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    number: Int = 0
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (lesson.completed) {
                DoneBadge(size = 36.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (number > 0) number.toString() else "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (number > 0) {
                    Text(
                        text = stringResource(R.string.lesson_ui_number_format, number),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = lesson.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = lesson.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Difficulty badge
                    Badge(containerColor = difficultyColor(lesson.difficulty)) {
                        Text(
                            text = difficultyLabel(lesson.difficulty),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }

                    // Duration badge
                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            text = stringResource(R.string.min_format, lesson.estimatedMinutes.toString()),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // Points badge
                    Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text(
                            text = stringResource(R.string.xp_format, lesson.points),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}
