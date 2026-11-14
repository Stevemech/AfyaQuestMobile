package com.afyaquest.app.presentation.videoplayer

import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.net.Uri
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.afyaquest.app.R
import com.afyaquest.app.domain.model.VideoModule
import com.afyaquest.app.presentation.components.ErrorState
import com.afyaquest.app.presentation.learn.LearnTopic
import com.afyaquest.app.presentation.navigation.Screen
import com.afyaquest.app.presentation.videomodules.VideoModulesViewModel
import com.afyaquest.app.sync.VideoDownloadManager
import com.afyaquest.app.ui.theme.AfyaSuccess
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import com.afyaquest.app.presentation.navigation.popBack

/** A video counts as watched once this fraction of it has been played. */
private const val WATCHED_FRACTION = 0.9

/**
 * Plays one video ([moduleId] is the video id). The video is marked watched only when it has
 * actually been played to (near) the end; on completion an end card offers the next step
 * (quiz, next video, or back to where the video was opened from) so the player never dead-ends.
 *
 * [topicId] is set when the video was opened from a focus topic or the featured row: "Next video"
 * and the "Video 2 of 7" position then follow that topic instead of the video's numbered module.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    moduleId: String,
    navController: NavController,
    topicId: String? = null,
    viewModel: VideoModulesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val watchedVideos by viewModel.watchedVideos.collectAsState()

    val videoUrl = remember(moduleId) { viewModel.getVideoUrl(moduleId) }
    val videoTitle = remember(moduleId) { viewModel.getVideoTitle(moduleId) }
    val moduleNumber = remember(moduleId) { viewModel.moduleNumberFor(moduleId) }
    // Only honour the topic when this video really belongs to it (guards a stale / edited route).
    val topic = remember(topicId, moduleId) {
        LearnTopic.fromId(topicId)?.takeIf { moduleId in it.videoIds }
    }
    val videoIndex = remember(moduleId, topic) {
        if (topic != null) viewModel.videoIndexInTopic(topic, moduleId)
        else viewModel.videoIndexInModule(moduleId)
    }
    val videoCount = remember(moduleId, topic) {
        topic?.videoIds?.size ?: moduleNumber?.let { viewModel.videoCountInModule(it) } ?: 0
    }
    val nextVideo = remember(watchedVideos, moduleId, topic) {
        if (topic != null) viewModel.nextUnwatchedInTopic(topic, moduleId)
        else moduleNumber?.let { viewModel.nextUnwatchedInModule(it, moduleId) }
    }
    // The end card's last action pops back, so name the place it actually returns to.
    val backLabelRes = remember(navController) {
        when (navController.previousBackStackEntry?.destination?.route) {
            Screen.ModuleDetail.route -> R.string.learn_back_to_module
            Screen.Topic.route -> R.string.learn_back_to_topic
            else -> R.string.go_back
        }
    }

    // Check for locally downloaded file first (offline playback)
    val localPath = remember(moduleId) {
        val videoDir = context.getExternalFilesDir(VideoDownloadManager.VIDEO_DIR)
        val localFile = videoDir?.resolve("module_$moduleId.mp4")
        if (localFile != null && localFile.exists() && localFile.length() > 0) {
            localFile.absolutePath
        } else {
            null
        }
    }

    // Prefer local file over streaming URL
    val mediaUri = remember(localPath, videoUrl) {
        localPath?.let { Uri.fromFile(File(it)) } ?: videoUrl?.let { Uri.parse(it) }
    }

    // Survives rotation so playback resumes where it was.
    var savedPosition by rememberSaveable { mutableLongStateOf(0L) }
    var savedPlayWhenReady by rememberSaveable { mutableStateOf(true) }
    var markedWatched by rememberSaveable { mutableStateOf(false) }

    var isPlaying by remember { mutableStateOf(false) }
    var isEnded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    val exoPlayer = remember(mediaUri) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        // The saved position is written once a second while a video plays. Read it here without
        // subscribing to it: otherwise this whole screen was recomposed every second.
        val (startAt, startPlaying) = Snapshot.withoutReadObservation { savedPosition to savedPlayWhenReady }
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            if (mediaUri != null) {
                setMediaItem(MediaItem.fromUri(mediaUri))
                prepare()
                if (startAt > 0L) seekTo(startAt)
                playWhenReady = startPlaying
            }
        }
    }

    // Leaving the app (Home button, screen off, a phone call) pauses the video; it used to keep
    // playing its sound in the background. Coming back resumes it if it was playing.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        var resumeOnReturn = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    resumeOnReturn = exoPlayer.isPlaying
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_START -> {
                    if (resumeOnReturn) exoPlayer.play()
                    resumeOnReturn = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        isEnded = true
                        if (!markedWatched) {
                            markedWatched = true
                            viewModel.markVideoWatched(moduleId)
                        }
                    }
                    Player.STATE_READY -> hasError = false
                    else -> Unit
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) isEnded = false
                savedPlayWhenReady = exoPlayer.playWhenReady
                savedPosition = exoPlayer.currentPosition
            }

            override fun onPlayerError(error: PlaybackException) {
                hasError = true
                isPlaying = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // While playing: remember the position (for rotation) and mark watched at 90%.
    LaunchedEffect(isPlaying, exoPlayer) {
        while (isActive && isPlaying) {
            delay(1000)
            val position = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            savedPosition = position
            if (!markedWatched && duration > 0L && position >= duration * WATCHED_FRACTION) {
                markedWatched = true
                viewModel.markVideoWatched(moduleId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = videoTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val collectionLabel = when {
                            topic != null -> stringResource(topic.titleRes)
                            moduleNumber != null ->
                                stringResource(R.string.learn_module_number_format, moduleNumber)
                            else -> null
                        }
                        if (collectionLabel != null && videoIndex != null && videoCount > 0) {
                            Text(
                                text = collectionLabel +
                                    " · " +
                                    stringResource(R.string.learn_video_of_format, videoIndex, videoCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (mediaUri == null) {
                Text(
                    text = stringResource(R.string.video_not_available),
                    color = Color.White
                )
            } else {
                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            player = exoPlayer
                            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                            // A spinner while the video loads. Without it a slow connection
                            // showed a plain black screen for many seconds, as if it had hung.
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        }
                    },
                    update = { view -> view.player = exoPlayer },
                    modifier = Modifier.fillMaxSize()
                )

                if (hasError) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            ErrorState(
                                message = stringResource(R.string.learn_video_error),
                                onRetry = {
                                    hasError = false
                                    exoPlayer.prepare()
                                    exoPlayer.play()
                                }
                            )
                        }
                    }
                } else if (isEnded) {
                    VideoEndCard(
                        nextVideo = nextVideo,
                        backLabel = stringResource(backLabelRes),
                        onTakeQuiz = {
                            navController.navigate(Screen.ModuleQuiz.createRoute(moduleId)) {
                                popUpTo(Screen.VideoPlayer.route) { inclusive = true }
                            }
                        },
                        onNextVideo = { next ->
                            navController.navigate(Screen.VideoPlayer.createRoute(next.id, topic?.id)) {
                                popUpTo(Screen.VideoPlayer.route) { inclusive = true }
                            }
                        },
                        onWatchAgain = {
                            isEnded = false
                            exoPlayer.seekTo(0L)
                            exoPlayer.play()
                        },
                        onBack = { navController.popBack() }
                    )
                }
            }
        }
    }
}

/** Overlay shown when playback reaches the end: celebrate, then one clear next step. */
@Composable
private fun VideoEndCard(
    nextVideo: VideoModule?,
    backLabel: String,
    onTakeQuiz: () -> Unit,
    onNextVideo: (VideoModule) -> Unit,
    onWatchAgain: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = AfyaSuccess,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.learn_video_complete),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onTakeQuiz,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.take_quiz))
                }

                if (nextVideo != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onNextVideo(nextVideo) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.learn_next_video))
                    }
                    Text(
                        text = nextVideo.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onWatchAgain,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.watch_again))
                }
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(backLabel)
                }
            }
        }
    }
}
