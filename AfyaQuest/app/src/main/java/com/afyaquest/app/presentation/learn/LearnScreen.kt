package com.afyaquest.app.presentation.learn

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.afyaquest.app.R
import com.afyaquest.app.presentation.lessons.LessonsContent
import com.afyaquest.app.presentation.navigation.Screen
import com.afyaquest.app.presentation.navigation.navigateSingle
import com.afyaquest.app.presentation.videomodules.VideoModulesContent

/**
 * Learn hub: one top-level destination (bottom bar) with two tabs, Videos and Lessons.
 * No back arrow - this is a root screen. "Ask Fred" is always one tap away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    navController: NavController,
    initialTab: String
) {
    var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    val isVideos = selectedTab != Screen.Learn.TAB_LESSONS

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.learn_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigateSingle(Screen.Chat.route) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Chat,
                            contentDescription = stringResource(R.string.ask_fred)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = if (isVideos) 0 else 1) {
                LeadingIconTab(
                    selected = isVideos,
                    onClick = { selectedTab = Screen.Learn.TAB_VIDEOS },
                    text = { Text(stringResource(R.string.learn_tab_videos)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.PlayCircle,
                            contentDescription = null
                        )
                    }
                )
                LeadingIconTab(
                    selected = !isVideos,
                    onClick = { selectedTab = Screen.Learn.TAB_LESSONS },
                    text = { Text(stringResource(R.string.learn_tab_lessons)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null
                        )
                    }
                )
            }

            if (isVideos) {
                VideoModulesContent(
                    navController = navController,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LessonsContent(
                    navController = navController,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}
