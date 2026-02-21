package com.example.afyaquest.presentation.videoplayer

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.afyaquest.presentation.videomodules.VideoModulesViewModel
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    moduleId: String,
    navController: NavController,
    viewModel: VideoModulesViewModel
) {
    val context = LocalContext.current
    val videoUrl = viewModel.getVideoUrl(moduleId)

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            if (videoUrl != null) {
                setMediaItem(MediaItem.fromUri(videoUrl))
                prepare()
                playWhenReady = true
            }
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        viewModel.markVideoWatched(moduleId)
                    }
                }
            })
        }
    }

    // Mark watched when the player starts playing (not just on completion)
    LaunchedEffect(Unit) {
        viewModel.markVideoWatched(moduleId)
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.getVideoTitle(moduleId)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            if (videoUrl != null) {
                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            player = exoPlayer
                            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "Video not available",
                    color = Color.White
                )
            }
        }
    }
}
