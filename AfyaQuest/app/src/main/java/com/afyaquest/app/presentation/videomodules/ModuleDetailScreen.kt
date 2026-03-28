package com.afyaquest.app.presentation.videomodules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.navigation.NavController
import com.afyaquest.app.R
import com.afyaquest.app.domain.model.VideoModule
import com.afyaquest.app.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailScreen(
    moduleNumber: Int,
    navController: NavController,
    viewModel: VideoModulesViewModel
) {
    val watchedVideos by viewModel.watchedVideos.collectAsState()
    val completedQuizzes by viewModel.completedQuizzes.collectAsState()
    val videos = viewModel.getVideosForModule(moduleNumber)
    val moduleTitle = VideoModulesViewModel.MODULE_TITLES[moduleNumber] ?: "Module $moduleNumber"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Module $moduleNumber", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Text(moduleTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
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
            val watchedCount = videos.count { it.watched }
            val quizCount = videos.count { it.quizComplete }

            ModuleProgressCard(
                watchedCount = watchedCount,
                quizCount = quizCount,
                totalVideos = videos.size
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(videos) { video ->
                    VideoCard(
                        video = video,
                        onVideoClick = {
                            navController.navigate(Screen.VideoPlayer.createRoute(video.id))
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

@Composable
fun ModuleProgressCard(
    watchedCount: Int,
    quizCount: Int,
    totalVideos: Int
) {
    val progress = if (totalVideos > 0) watchedCount.toFloat() / totalVideos else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$watchedCount/$totalVideos videos watched",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "$quizCount/$totalVideos quizzes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF438894),
            )
        }
    }
}

@Composable
fun VideoCard(
    video: VideoModule,
    onVideoClick: () -> Unit,
    onQuizClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = video.thumbnail, fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = video.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (video.watched) {
                            Badge(containerColor = Color(0xFF438894)) {
                                Text(
                                    text = stringResource(R.string.watched),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                        if (video.quizComplete) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
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
