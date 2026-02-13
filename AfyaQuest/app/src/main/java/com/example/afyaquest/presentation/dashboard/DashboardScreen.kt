package com.example.afyaquest.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.afyaquest.presentation.auth.AuthViewModel
import com.example.afyaquest.presentation.navigation.Screen
import com.example.afyaquest.presentation.components.SyncStatusIndicator

/**
 * Dashboard screen - main hub of the app
 * Displays user stats, daily tasks, and learning center
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val xpData by dashboardViewModel.xpData.collectAsState()
    val scrollState = rememberScrollState()
    val isConnected by dashboardViewModel.isConnected.collectAsState()
    val unsyncedCount by dashboardViewModel.unsyncedCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Afya Quest", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Screen.Profile.route)
                    }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                    IconButton(onClick = {
                        navController.navigate(Screen.Settings.route)
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Sync Status Indicator
            SyncStatusIndicator(
                isConnected = isConnected,
                unsyncedCount = unsyncedCount,
                onSyncClick = { dashboardViewModel.triggerSync() }
            )

            // Stats Header
            StatsHeader(
                streak = xpData.streak,
                xp = xpData.totalXP,
                lives = xpData.lives
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User Level Card
            UserLevelCard(
                level = xpData.level,
                rank = xpData.rank,
                progress = dashboardViewModel.getLevelProgress(),
                xpForNextLevel = dashboardViewModel.getXPForNextLevel()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Daily To-Do Section
            DailyTasksSection(navController)

            Spacer(modifier = Modifier.height(24.dp))

            // Learning Center Section
            LearningCenterSection(navController)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatsHeader(
    streak: Int,
    xp: Int,
    lives: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Streak
        StatItem(
            icon = "🔥",
            value = streak.toString(),
            label = "Streak"
        )

        // XP
        StatItem(
            icon = "💎",
            value = "$xp XP",
            label = "Total XP"
        )

        // Lives
        StatItem(
            icon = "❤️",
            value = lives.toString(),
            label = "Lives"
        )
    }
}

@Composable
fun StatItem(
    icon: String,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 32.sp
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun UserLevelCard(
    level: Int,
    rank: String,
    progress: Float,
    xpForNextLevel: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Level $level",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = rank,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Level badge
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = level.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Column {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$xpForNextLevel XP to Level ${level + 1}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DailyTasksSection(navController: NavController) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Daily To-Do",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Daily Questions Task
        TaskCard(
            icon = "📊",
            title = "Daily Questions!",
            description = "Answer your three daily questions to collect your daily XP!",
            isRequired = true,
            onClick = {
                navController.navigate(Screen.DailyQuestions.route)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Daily Itinerary Task
        TaskCard(
            icon = "🗺️",
            title = "Daily Itinerary",
            description = "Find your scheduled locations and patients here!",
            isRequired = true,
            onClick = {
                navController.navigate(Screen.Map.route)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Daily Report Task
        TaskCard(
            icon = "📝",
            title = "Daily Report",
            description = "Fill out the report at the end of the day.",
            isRequired = true,
            onClick = {
                navController.navigate(Screen.DailyReport.route)
            }
        )
    }
}

@Composable
fun LearningCenterSection(navController: NavController) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Learning Center",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Video Modules Task
        TaskCard(
            icon = "🎬",
            title = "Video Modules",
            description = "Watch educational videos and take quizzes!",
            isRequired = false,
            onClick = {
                navController.navigate(Screen.VideoModules.route)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Lessons Task
        TaskCard(
            icon = "📚",
            title = "Interactive Lessons",
            description = "Read lessons and earn XP by completing them!",
            isRequired = false,
            onClick = {
                navController.navigate(Screen.Lessons.route)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // AI Chat Assistant
        TaskCard(
            icon = "💬",
            title = "Chat with Fred",
            description = "Ask Fred, your AI health assistant, any questions!",
            isRequired = false,
            onClick = {
                navController.navigate(Screen.Chat.route)
            }
        )
    }
}

@Composable
fun TaskCard(
    icon: String,
    title: String,
    description: String,
    isRequired: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isRequired) {
                        Text(
                            text = " *",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
