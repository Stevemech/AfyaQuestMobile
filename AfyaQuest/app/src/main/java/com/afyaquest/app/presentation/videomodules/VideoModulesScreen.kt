package com.afyaquest.app.presentation.videomodules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.afyaquest.app.domain.model.VideoModuleFolder
import com.afyaquest.app.presentation.components.DoneBadge
import com.afyaquest.app.presentation.components.EmptyState
import com.afyaquest.app.presentation.components.NextStepCard
import com.afyaquest.app.presentation.components.ProgressSummary
import com.afyaquest.app.presentation.navigation.Screen
import com.afyaquest.app.presentation.navigation.navigateSingle
import com.afyaquest.app.ui.theme.AfyaSuccess

/**
 * Videos tab of the Learn hub: overall progress, one clear "continue" action, then the six
 * modules as numbered steps. No Scaffold here - the hub owns the top bar and tabs.
 */
@Composable
fun VideoModulesContent(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: VideoModulesViewModel = hiltViewModel()
) {
    val watchedVideos by viewModel.watchedVideos.collectAsState()
    val completedQuizzes by viewModel.completedQuizzes.collectAsState()

    // Re-derive everything whenever the persisted progress changes so returning from the
    // player / quiz updates the list immediately.
    val moduleFolders = remember(watchedVideos, completedQuizzes) { viewModel.getModuleFolders() }
    val nextVideo = remember(watchedVideos) { viewModel.nextUnwatchedVideo() }
    val totalVideos = viewModel.getTotalVideos()
    val watchedCount = remember(watchedVideos) { viewModel.getWatchedCount() }
    val quizCount = remember(completedQuizzes) { viewModel.getQuizCompletedCount() }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "overall") {
            OverallProgressCard(
                watchedCount = watchedCount,
                quizCount = quizCount,
                totalVideos = totalVideos
            )
        }

        item(key = "next") {
            if (nextVideo != null) {
                NextVideoCard(
                    video = nextVideo,
                    onClick = {
                        // Build Module -> Player so back returns to the module, not the hub.
                        navController.navigateSingle(Screen.ModuleDetail.createRoute(nextVideo.moduleNumber))
                        navController.navigateSingle(Screen.VideoPlayer.createRoute(nextVideo.id))
                    }
                )
            } else {
                AllVideosDoneCard()
            }
        }

        item(key = "modules_header") {
            Text(
                text = stringResource(R.string.modules),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(moduleFolders, key = { it.moduleNumber }) { folder ->
            ModuleStepCard(
                folder = folder,
                isNext = nextVideo?.moduleNumber == folder.moduleNumber,
                onClick = {
                    navController.navigateSingle(Screen.ModuleDetail.createRoute(folder.moduleNumber))
                }
            )
        }
    }
}

@Composable
private fun OverallProgressCard(
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
            Text(
                text = stringResource(R.string.progress),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
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
private fun NextVideoCard(video: VideoModule, onClick: () -> Unit) {
    val moduleTitle = stringResource(VideoModulesViewModel.moduleTitleRes(video.moduleNumber))
    val moduleLabel = stringResource(R.string.learn_module_number_format, video.moduleNumber)
    NextStepCard(
        title = video.title,
        subtitle = "$moduleLabel · $moduleTitle",
        actionText = stringResource(R.string.continue_label),
        onClick = onClick,
        icon = Icons.Filled.PlayArrow,
        eyebrow = stringResource(R.string.learn_continue_learning)
    )
}

@Composable
private fun AllVideosDoneCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        EmptyState(
            title = stringResource(R.string.learn_all_videos_done),
            message = stringResource(R.string.learn_all_videos_done_hint),
            icon = Icons.Outlined.CheckCircle
        )
    }
}

/**
 * One module as a numbered step: circle with the module number, title, per-module progress bar,
 * a DoneBadge once every video and quiz is complete, and a "Next up" eyebrow on the module that
 * holds the next unwatched video.
 */
@Composable
private fun ModuleStepCard(
    folder: VideoModuleFolder,
    isNext: Boolean,
    onClick: () -> Unit
) {
    val total = folder.videoCount
    val isComplete = total > 0 && folder.watchedCount >= total && folder.quizzesCompleted >= total
    // Progress counts both halves of each video: watched + quiz done.
    val progress = if (total > 0) {
        ((folder.watchedCount + folder.quizzesCompleted).toFloat() / (total * 2)).coerceIn(0f, 1f)
    } else 0f

    val containerColor = when {
        isNext -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val circleColor = when {
        isComplete -> AfyaSuccess
        isNext -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val circleTextColor = when {
        isComplete -> MaterialTheme.colorScheme.onPrimary
        isNext -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 3.dp else 1.dp),
        border = if (isNext) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(circleColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = folder.moduleNumber.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = circleTextColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isNext) stringResource(R.string.next_up)
                        else stringResource(R.string.learn_module_number_format, folder.moduleNumber),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    if (isComplete) {
                        DoneBadge(contentDescription = stringResource(R.string.learn_module_complete))
                    }
                }
                Text(
                    text = stringResource(VideoModulesViewModel.moduleTitleRes(folder.moduleNumber)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(VideoModulesViewModel.moduleDescriptionRes(folder.moduleNumber)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isComplete) AfyaSuccess else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.learn_videos_count_format, folder.watchedCount, total) +
                        " • " +
                        stringResource(R.string.learn_quizzes_count_format, folder.quizzesCompleted, total),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
