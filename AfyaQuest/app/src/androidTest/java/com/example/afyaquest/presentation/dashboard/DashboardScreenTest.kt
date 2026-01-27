package com.example.afyaquest.presentation.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.afyaquest.ui.theme.AfyaQuestTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for DashboardScreen
 */
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardScreen_displaysTitle() {
        composeTestRule.setContent {
            AfyaQuestTheme {
                val navController = rememberNavController()
                DashboardScreen(navController = navController)
            }
        }

        composeTestRule.onNodeWithText("Afya Quest").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_displaysStatsHeader() {
        composeTestRule.setContent {
            AfyaQuestTheme {
                val navController = rememberNavController()
                DashboardScreen(navController = navController)
            }
        }

        // Check for stats labels
        composeTestRule.onNodeWithText("Streak").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total XP").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lives").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_displaysDailyTasks() {
        composeTestRule.setContent {
            AfyaQuestTheme {
                val navController = rememberNavController()
                DashboardScreen(navController = navController)
            }
        }

        composeTestRule.onNodeWithText("Daily To-Do").assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily Questions!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily Itinerary").assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily Report").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_displaysLearningCenter() {
        composeTestRule.setContent {
            AfyaQuestTheme {
                val navController = rememberNavController()
                DashboardScreen(navController = navController)
            }
        }

        composeTestRule.onNodeWithText("Learning Center").assertIsDisplayed()
        composeTestRule.onNodeWithText("Video Modules").assertIsDisplayed()
        composeTestRule.onNodeWithText("Interactive Lessons").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chat with Steve").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_taskCardsAreClickable() {
        composeTestRule.setContent {
            AfyaQuestTheme {
                val navController = rememberNavController()
                DashboardScreen(navController = navController)
            }
        }

        // Verify task cards have click actions
        composeTestRule.onNodeWithText("Daily Questions!")
            .assertHasClickAction()

        composeTestRule.onNodeWithText("Video Modules")
            .assertHasClickAction()
    }
}
