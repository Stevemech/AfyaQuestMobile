package com.afyaquest.app.presentation.videomodules

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.afyaquest.app.R
import com.afyaquest.app.domain.model.VideoModule
import com.afyaquest.app.presentation.components.DoneBadge
import com.afyaquest.app.presentation.components.HintRow
import com.afyaquest.app.presentation.components.ProgressSummary
import com.afyaquest.app.presentation.navigation.Screen
import com.afyaquest.app.presentation.navigation.navigateSingle
import com.afyaquest.app.ui.theme.AfyaSuccess

/**
 * One module: "Module N" eyebrow + localized title, module progress, and the videos as
 * numbered steps. The first unwatched video is highlighted as "Next up"; the quiz button stays
 * locked (with a hint) until its video has been watched.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailScreen(
    moduleNumber: Int,
    navController: NavController,
    viewModel: VideoModulesViewModel = hiltViewModel()
) {
    val watchedVideos by viewModel.watchedVideos.collectAsState()
    val completedQuizzes by viewModel.completedQuizzes.collectAsState()
    val videos = remember(watchedVideos, completedQuizzes, moduleNumber) {
        viewModel.getVideosForModule(moduleNumber)
    }
    val nextVideoId = remember(videos) { videos.firstOrNull { !it.watched }?.id }
    val moduleTitle = stringResource(VideoModulesViewModel.moduleTitleRes(moduleNumber))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.learn_module_number_format, moduleNumber),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = moduleTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val watchedCount = videos.count { it.watched }
        val quizCount = videos.count { it.quizComplete }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "progress") {
                ModuleProgressCard(
                    watchedCount = watchedCount,
                    quizCount = quizCount,
                    totalVideos = videos.size
                )
            }

            itemsIndexed(videos, key = { _, video -> video.id }) { index, video ->
                VideoStepCard(
                    stepNumber = index + 1,
                    video = video,
                    isNext = video.id == nextVideoId,
                    onVideoClick = {
                        navController.navigateSingle(Screen.VideoPlayer.createRoute(video.id))
                    },
                    onQuizClick = {
                        navController.navigateSingle(Screen.ModuleQuiz.createRoute(video.id))
                    }
                )
            }
        }
    }
}

@Composable
private fun ModuleProgressCard(
    watchedCount: Int,
    quizCount: Int,
    totalVideos: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ProgressSummary(
                label = stringResource(R.string.videos_watched),
                done = watchedCount,
                total = totalVideos
            )
            Spacer(modifier = Modifier.height(12.dp))
            ProgressSummary(
                label = stringResource(R.string.quizzes_complete),
                done = quizCount,
                total = totalVideos,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun VideoStepCard(
    stepNumber: Int,
    video: VideoModule,
    isNext: Boolean,
    onVideoClick: () -> Unit,
    onQuizClick: () -> Unit
) {
    val isDone = video.watched && video.quizComplete
    val containerColor = if (isNext) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface
    val circleColor = when {
        isDone -> AfyaSuccess
        isNext -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val circleTextColor = when {
        isDone || isNext -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 3.dp else 1.dp),
        border = if (isNext) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(circleColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = circleTextColor
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (isNext) {
                        Text(
                            text = stringResource(R.string.next_up),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = video.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                    StatusRow(video = video, isDone = isDone)
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
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (video.watched) stringResource(R.string.watch_again)
                        else stringResource(R.string.watch_video)
                    )
                }
                if (video.hasQuiz) {
                    OutlinedButton(
                        onClick = onQuizClick,
                        modifier = Modifier.weight(1f),
                        enabled = video.watched
                    ) {
                        if (!video.watched) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            if (video.quizComplete) stringResource(R.string.retake_quiz)
                            else stringResource(R.string.take_quiz)
                        )
                    }
                }
            }

            if (video.hasQuiz && !video.watched) {
                Spacer(modifier = Modifier.height(8.dp))
                HintRow(
                    text = stringResource(R.string.learn_quiz_locked_hint),
                    icon = Icons.Outlined.Lock
                )
            }
        }
    }
}

/** Small status line under the video description: nothing, "Watched", or a DoneBadge. */
@Composable
private fun StatusRow(video: VideoModule, isDone: Boolean) {
    if (!video.watched && !video.quizComplete) return
    Spacer(modifier = Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isDone) {
            DoneBadge(size = 18.dp, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.completed),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AfyaSuccess
            )
        } else {
            Text(
                text = if (video.watched) stringResource(R.string.learn_watched)
                else stringResource(R.string.learn_quiz_done),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
