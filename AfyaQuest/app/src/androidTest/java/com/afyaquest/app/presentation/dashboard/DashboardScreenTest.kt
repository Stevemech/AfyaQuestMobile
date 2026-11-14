package com.afyaquest.app.presentation.dashboard

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afyaquest.app.HiltTestActivity
import com.afyaquest.app.R
import com.afyaquest.app.ui.theme.AfyaQuestTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the Home screen. The network is replaced by one that reaches nothing (see
 * TestNetworkModule), so this is Home as a CHV sees it with no connection.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    private fun text(id: Int) = composeTestRule.activity.getString(id)

    @Before
    fun showHome() {
        composeTestRule.setContent {
            AfyaQuestTheme {
                DashboardScreen(navController = rememberNavController())
            }
        }
        composeTestRule.waitForIdle()
    }

    /** Home is one long scrolling column: bring a row into view before asserting on it. */
    private fun scrollTo(label: String) {
        composeTestRule.onAllNodesWithText(label).onFirst().performScrollTo()
    }

    @Test
    fun dashboardScreen_displaysTitleAndTopBarActions() {
        composeTestRule.onNodeWithText(text(R.string.app_name)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(text(R.string.language)).assertHasClickAction()
    }

    @Test
    fun dashboardScreen_displaysStatsHeader() {
        composeTestRule.onNodeWithText(text(R.string.streak)).assertIsDisplayed()
        composeTestRule.onNodeWithText(text(R.string.total_xp)).assertIsDisplayed()
        composeTestRule.onNodeWithText(text(R.string.lives)).assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_displaysClockStatus() {
        // Either state is fine; what matters is that the control is there and can be used.
        val clockIn = composeTestRule.onAllNodesWithText(text(R.string.clock_in)).fetchSemanticsNodes()
        val clockOut = composeTestRule.onAllNodesWithText(text(R.string.clock_out)).fetchSemanticsNodes()
        assert(clockIn.size + clockOut.size == 1) { "expected exactly one clock in/out button" }
    }

    @Test
    fun dashboardScreen_displaysEmergencyGuideEntry() {
        scrollTo(text(R.string.emergency_response))
        composeTestRule.onNodeWithText(text(R.string.emergency_response)).assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_displaysDailyTasks() {
        listOf(R.string.daily_todo, R.string.daily_questions, R.string.daily_itinerary, R.string.daily_report)
            .map(::text)
            .forEach { label ->
                scrollTo(label)
                composeTestRule.onNodeWithText(label).assertIsDisplayed()
            }
    }

    @Test
    fun dashboardScreen_displaysLearningCenter() {
        listOf(
            R.string.learning_center,
            R.string.video_modules,
            R.string.interactive_lessons,
            R.string.chat_with_steve
        ).map(::text).forEach { label ->
            scrollTo(label)
            composeTestRule.onAllNodesWithText(label).onFirst().assertIsDisplayed()
        }
    }

    @Test
    fun dashboardScreen_showsAssignmentsSectionWithoutAConnection() {
        scrollTo(text(R.string.assigned_to_you))
        composeTestRule.onNodeWithText(text(R.string.assigned_to_you)).assertIsDisplayed()
    }
}
