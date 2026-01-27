package com.example.afyaquest.presentation.lessons

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.afyaquest.domain.model.Difficulty
import com.example.afyaquest.domain.model.Lesson
import com.example.afyaquest.domain.model.LessonCategory

/**
 * Interactive Lessons screen
 * Displays educational lessons with categories
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonsScreen(
    navController: NavController,
    viewModel: LessonsViewModel = hiltViewModel()
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedLesson by viewModel.selectedLesson.collectAsState()
    val filteredLessons = viewModel.getFilteredLessons()

    // Show lesson detail if a lesson is selected
    selectedLesson?.let { lesson ->
        LessonDetailScreen(
            lesson = lesson,
            onBack = { viewModel.selectLesson(null) },
            onComplete = { viewModel.completeLesson(lesson.id) }
        )
        return
    }

    // Show lessons list
    run {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Interactive Lessons", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                // Stats card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${viewModel.getCompletedCount()}/${viewModel.getTotalLessons()} Lessons Completed",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category filter
                Text(
                    text = "Categories",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.setCategory(null) },
                            label = { Text("All") }
                        )
                    }

                    items(viewModel.categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.setCategory(category) },
                            label = { Text(viewModel.getCategoryDisplayName(category)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lessons list
                if (filteredLessons.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No lessons available",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredLessons) { lesson ->
                            LessonCard(
                                lesson = lesson,
                                onClick = { viewModel.selectLesson(lesson) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonCard(
    lesson: Lesson,
    onClick: () -> Unit
) {
    val difficultyColor = when (lesson.difficulty) {
        Difficulty.EASY -> Color(0xFF4CAF50)
        Difficulty.MEDIUM -> Color(0xFFFF9800)
        Difficulty.HARD -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lesson.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (lesson.completed) {
                    Badge(
                        containerColor = Color(0xFF4CAF50)
                    ) {
                        Text(
                            text = "✓",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = lesson.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Difficulty badge
                Badge(
                    containerColor = difficultyColor
                ) {
                    Text(
                        text = lesson.difficulty.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }

                // Duration badge
                Badge(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "⏱️ ${lesson.estimatedMinutes} min",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                // Points badge
                Badge(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "💎 ${lesson.points} XP",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(
    lesson: Lesson,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val scrollState = rememberScrollState()
    var hasCompleted by remember { mutableStateOf(lesson.completed) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lesson", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (!hasCompleted) {
                Surface(
                    tonalElevation = 3.dp
                ) {
                    Button(
                        onClick = {
                            onComplete()
                            hasCompleted = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Mark as Complete (+${lesson.points} XP)")
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
            // Lesson title
            Text(
                text = lesson.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val difficultyColor = when (lesson.difficulty) {
                    Difficulty.EASY -> Color(0xFF4CAF50)
                    Difficulty.MEDIUM -> Color(0xFFFF9800)
                    Difficulty.HARD -> Color(0xFFF44336)
                }

                Badge(containerColor = difficultyColor) {
                    Text(
                        text = lesson.difficulty.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White
                    )
                }

                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        text = "${lesson.estimatedMinutes} min",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

            if (hasCompleted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✓", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Lesson completed!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
