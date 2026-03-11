package com.example.afyaquest.presentation.videomodules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.afyaquest.R
import com.example.afyaquest.domain.model.VideoModuleFolder
import com.example.afyaquest.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoModulesScreen(
    navController: NavController,
    viewModel: VideoModulesViewModel = hiltViewModel()
) {
    val watchedVideos by viewModel.watchedVideos.collectAsState()
    val completedQuizzes by viewModel.completedQuizzes.collectAsState()
    val moduleFolders = viewModel.getModuleFolders()

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
            OverallStatsCard(
                watchedCount = viewModel.getWatchedCount(),
                quizCompletedCount = viewModel.getQuizCompletedCount(),
                totalVideos = viewModel.getTotalVideos()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Modules",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(moduleFolders) { folder ->
                    ModuleFolderCard(
                        folder = folder,
                        onClick = {
                            navController.navigate(Screen.ModuleDetail.createRoute(folder.moduleNumber))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OverallStatsCard(
    watchedCount: Int,
    quizCompletedCount: Int,
    totalVideos: Int
) {
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$watchedCount/$totalVideos",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.videos_watched),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$quizCompletedCount/$totalVideos",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.quizzes_complete),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ModuleFolderCard(
    folder: VideoModuleFolder,
    onClick: () -> Unit
) {
    val progress = if (folder.videoCount > 0) folder.watchedCount.toFloat() / folder.videoCount else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = folder.icon, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Module ${folder.moduleNumber}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = folder.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp
                )
                Text(
                    text = folder.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF438894),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${folder.watchedCount}/${folder.videoCount} videos \u2022 ${folder.quizzesCompleted}/${folder.videoCount} quizzes",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
