package com.example.afyaquest.presentation.videomodules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.afyaquest.R
import com.example.afyaquest.domain.model.VideoCategory
import com.example.afyaquest.domain.model.VideoModule
import com.example.afyaquest.presentation.navigation.Screen

/**
 * Video Modules screen
 * Displays learning videos with categories and progress tracking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoModulesScreen(
    navController: NavController,
    viewModel: VideoModulesViewModel = hiltViewModel()
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val watchedVideos by viewModel.watchedVideos.collectAsState()
    val completedQuizzes by viewModel.completedQuizzes.collectAsState()
    val filteredVideos = viewModel.getFilteredVideos()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.video_modules), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            StatsCard(
                watchedCount = viewModel.getWatchedCount(),
                quizCompletedCount = viewModel.getQuizCompletedCount(),
                totalVideos = viewModel.getTotalVideos()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category filter
            Text(
                text = stringResource(R.string.categories),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All categories option
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text(stringResource(R.string.all)) }
                    )
                }

                // Individual categories
                items(viewModel.categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.setCategory(category) },
                        label = { Text(viewModel.getCategoryDisplayName(category)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Videos list
            if (filteredVideos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_videos_available),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredVideos) { video ->
                        VideoModuleCard(
                            video = video,
                            onVideoClick = {
                                if (video.videoUrl != null) {
                                    navController.navigate(Screen.VideoPlayer.createRoute(video.id))
                                } else {
                                    viewModel.markVideoWatched(video.id)
                                }
                            },
                            onQuizClick = {
                                navController.navigate(Screen.ModuleQuiz.createRoute(video.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCard(
    watchedCount: Int,
    quizCompletedCount: Int,
    totalVideos: Int
) {
    val videosWatchedLabel = stringResource(R.string.videos_watched)
    val quizzesCompleteLabel = stringResource(R.string.quizzes_complete)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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
            StatItem(
                value = "$watchedCount/$totalVideos",
                label = videosWatchedLabel
            )
            StatItem(
                value = "$quizCompletedCount/$totalVideos",
                label = quizzesCompleteLabel
            )
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun VideoModuleCard(
    video: VideoModule,
    onVideoClick: () -> Unit,
    onQuizClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = video.thumbnail,
                        fontSize = 40.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Video info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = video.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = video.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Duration badge
                        Badge(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = "⏱️ ${video.duration}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        // Watched indicator
                        if (video.watched) {
                            Badge(
                                containerColor = Color(0xFF4CAF50),
                                modifier = Modifier.padding(0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.watched),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Quiz complete indicator
                        if (video.quizComplete) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.quiz_check),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onVideoClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (video.watched) stringResource(R.string.watch_again) else stringResource(R.string.watch_video))
                }

                if (video.hasQuiz) {
                    OutlinedButton(
                        onClick = onQuizClick,
                        modifier = Modifier.weight(1f),
                        enabled = video.watched
                    ) {
                        Text(if (video.quizComplete) stringResource(R.string.retake_quiz) else stringResource(R.string.take_quiz))
                    }
                }
            }
        }
    }
}
