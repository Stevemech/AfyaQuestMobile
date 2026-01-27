package com.example.afyaquest.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.afyaquest.domain.model.Achievement
import com.example.afyaquest.domain.model.AchievementCategory

/**
 * Profile screen with tabs for Overview, Achievements, and Reflections
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val xpData by viewModel.xpData.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
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
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    text = { Text("Overview") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = { Text("Achievements") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    text = { Text("Reflections") }
                )
            }

            // Tab content
            when (selectedTab) {
                0 -> OverviewTab(xpData = xpData)
                1 -> AchievementsTab(achievements = achievements)
                2 -> ReflectionsTab(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun OverviewTab(xpData: com.example.afyaquest.util.XpData) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = xpData.level.toString(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Level ${xpData.level}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = xpData.rank,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Stats grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "💎",
                    value = "${xpData.totalXP}",
                    label = "Total XP"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "🔥",
                    value = "${xpData.streak}",
                    label = "Day Streak"
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "❤️",
                    value = "${xpData.lives}",
                    label = "Lives"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "🎯",
                    value = "${xpData.level}",
                    label = "Level"
                )
            }
        }

        // Quick stats
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Quick Stats",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    StatRow(label = "Lessons Completed", value = "6")
                    StatRow(label = "Videos Watched", value = "4")
                    StatRow(label = "Quizzes Taken", value = "12")
                    StatRow(label = "Reports Submitted", value = "8")
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: String,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AchievementsTab(achievements: List<Achievement>) {
    val unlockedAchievements = achievements.filter { it.unlocked }
    val lockedAchievements = achievements.filter { !it.unlocked }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Unlocked achievements
        item {
            Text(
                text = "Unlocked (${unlockedAchievements.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(unlockedAchievements) { achievement ->
            AchievementCard(achievement = achievement)
        }

        // Locked achievements
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Locked (${lockedAchievements.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(lockedAchievements) { achievement ->
            AchievementCard(achievement = achievement)
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.unlocked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.unlocked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = achievement.icon,
                    fontSize = 32.sp,
                    color = if (achievement.unlocked) Color.Unspecified else Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = achievement.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!achievement.unlocked && achievement.target > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { achievement.progress.toFloat() / achievement.target },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                    )
                    Text(
                        text = "${achievement.progress}/${achievement.target}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (achievement.unlocked && achievement.unlockedDate != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Unlocked on ${achievement.unlockedDate}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Unlocked badge
            if (achievement.unlocked) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Unlocked",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ReflectionsTab(viewModel: ProfileViewModel) {
    val reflections by viewModel.weeklyReflections.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Weekly Reflections",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (reflections.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No reflections yet. Start submitting weekly reflections to track your progress!",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(reflections) { reflection ->
                ReflectionCard(reflection = reflection)
            }
        }
    }
}

@Composable
fun ReflectionCard(reflection: com.example.afyaquest.domain.model.WeeklyReflection) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Week of ${reflection.weekStartDate}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    repeat(5) { index ->
                        Text(
                            text = if (index < reflection.overallRating) "⭐" else "☆",
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Text(
                text = "Success: ${reflection.successStory}",
                fontSize = 14.sp
            )
            Text(
                text = "Submitted: ${reflection.submittedDate}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
