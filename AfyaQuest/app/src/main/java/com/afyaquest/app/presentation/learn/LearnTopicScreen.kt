package com.afyaquest.app.presentation.learn

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.afyaquest.app.presentation.components.EmptyState
import com.afyaquest.app.presentation.components.ProgressSummary
import com.afyaquest.app.presentation.lessons.LessonCard
import com.afyaquest.app.presentation.lessons.LessonsViewModel
import com.afyaquest.app.presentation.navigation.Screen
import com.afyaquest.app.presentation.navigation.navigateSingle
import com.afyaquest.app.presentation.videomodules.VideoModulesViewModel
import com.afyaquest.app.presentation.videomodules.VideoStepCard
import com.afyaquest.app.presentation.navigation.popBack

/**
 * One focus topic ([LearnTopic]): what it covers, how much of it is done, then its videos as
 * numbered steps (the first unwatched one is "Next up") followed by its lessons. Videos open in
 * the player with the topic as context, so "Next video" stays on the subject.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnTopicScreen(
    topicId: String?,
    navController: NavController,
    videosViewModel: VideoModulesViewModel = hiltViewModel(),
    lessonsViewModel: LessonsViewModel = hiltViewModel()
) {
    val topic = remember(topicId) { LearnTopic.fromId(topicId) }

    val watchedVideos by videosViewModel.watchedVideos.collectAsState()
    val completedQuizzes by videosViewModel.completedQuizzes.collectAsState()
    val allLessons by lessonsViewModel.lessons.collectAsState()
    val completedLessons by lessonsViewModel.completedLessons.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.learn_topic_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(topic?.titleRes ?: R.string.learn_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (topic == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = stringResource(R.string.learn_topic_not_found),
                    icon = Icons.Outlined.Info,
                    actionText = stringResource(R.string.go_back),
                    onAction = { navController.popBack() }
                )
            }
            return@Scaffold
        }

        val videos = remember(topic, watchedVideos, completedQuizzes) {
            videosViewModel.getVideosForTopic(topic)
        }
        val nextVideoId = remember(videos) { videos.firstOrNull { !it.watched }?.id }
        // Keep the lesson's number from the Lessons tab ("Lesson 3") so it reads the same here.
        val lessons = remember(topic, allLessons, completedLessons) {
            topic.lessonIds.mapNotNull { id ->
                val index = allLessons.indexOfFirst { it.id == id }
                if (index < 0) null
                else (index + 1) to allLessons[index].copy(completed = id in completedLessons)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "summary") {
                TopicSummaryCard(
                    topic = topic,
                    videosWatched = videos.count { it.watched },
                    videosTotal = videos.size,
                    lessonsCompleted = lessons.count { it.second.completed },
                    lessonsTotal = lessons.size
                )
            }

            if (videos.isNotEmpty()) {
                item(key = "videos_header") {
                    SectionHeader(text = stringResource(R.string.learn_tab_videos))
                }
                itemsIndexed(videos, key = { _, video -> video.id }) { index, video ->
                    VideoStepCard(
                        stepNumber = index + 1,
                        video = video,
                        isNext = video.id == nextVideoId,
                        onVideoClick = {
                            navController.navigateSingle(Screen.VideoPlayer.createRoute(video.id, topic.id))
                        },
                        onQuizClick = {
                            navController.navigateSingle(Screen.ModuleQuiz.createRoute(video.id))
                        },
                        sourceLabel = stringResource(R.string.learn_module_number_format, video.moduleNumber) +
                            " · " + stringResource(VideoModulesViewModel.moduleTitleRes(video.moduleNumber))
                    )
                }
            }

            if (lessons.isNotEmpty()) {
                item(key = "lessons_header") {
                    SectionHeader(text = stringResource(R.string.learn_tab_lessons))
                }
                items(lessons, key = { (_, lesson) -> lesson.id }) { (number, lesson) ->
                    LessonCard(
                        lesson = lesson,
                        onClick = {
                            navController.navigateSingle(Screen.LessonDetail.createRoute(lesson.id))
                        },
                        number = number
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

/** Icon + description of the topic, then progress through its videos and lessons. */
@Composable
private fun TopicSummaryCard(
    topic: LearnTopic,
    videosWatched: Int,
    videosTotal: Int,
    lessonsCompleted: Int,
    lessonsTotal: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = topicIcon(topic),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(topic.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (videosTotal > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                ProgressSummary(
                    label = stringResource(R.string.videos_watched),
                    done = videosWatched,
                    total = videosTotal
                )
            }
            if (lessonsTotal > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                ProgressSummary(
                    label = stringResource(R.string.lesson_ui_progress_label),
                    done = lessonsCompleted,
                    total = lessonsTotal,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
