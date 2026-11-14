package com.afyaquest.app.presentation.learn

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afyaquest.app.presentation.lessons.LessonMarkdown
import com.afyaquest.app.presentation.videomodules.VideoModulesViewModel
import com.afyaquest.app.ui.theme.AfyaQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device tests for the featured-video / focus-topic cards and the lesson text renderer.
 * These composables take plain data (no ViewModel), so they run without the Hilt graph.
 */
@RunWith(AndroidJUnit4::class)
class LearnComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val featured = VideoModulesViewModel.featuredVideos().first()

    @Test
    fun featuredCard_showsVideoTopicAndModule_andOpensOnTap() {
        var taps = 0
        composeTestRule.setContent {
            AfyaQuestTheme {
                FeaturedVideoCard(
                    video = featured,
                    topicTitle = "Maternal Health",
                    moduleLabel = "Module 1 · Body Systems",
                    onClick = { taps++ }
                )
            }
        }

        composeTestRule.onNodeWithText("Female Reproductive System").assertIsDisplayed()
        composeTestRule.onNodeWithText("Maternal Health").assertIsDisplayed()
        composeTestRule.onNodeWithText("Module 1 · Body Systems").assertIsDisplayed()
        // Not watched yet: no "Watched" badge.
        composeTestRule.onAllNodesWithContentDescription("Watched").assertCountEquals(0)

        composeTestRule.onNodeWithText("Female Reproductive System").performClick()
        composeTestRule.runOnIdle { assertEquals(1, taps) }
    }

    @Test
    fun featuredCard_marksAWatchedVideo() {
        composeTestRule.setContent {
            AfyaQuestTheme {
                FeaturedVideoCard(
                    video = featured.copy(watched = true),
                    topicTitle = null,
                    moduleLabel = "Module 1 · Body Systems",
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Watched").assertIsDisplayed()
    }

    @Test
    fun compactFeaturedCard_showsEyebrowAndTitle_andOpensOnTap() {
        var taps = 0
        composeTestRule.setContent {
            AfyaQuestTheme {
                FeaturedVideoCompactCard(
                    video = featured,
                    eyebrow = "Featured · Maternal Health",
                    onClick = { taps++ }
                )
            }
        }

        composeTestRule.onNodeWithText("Featured · Maternal Health").assertIsDisplayed()
        composeTestRule.onNodeWithText("Female Reproductive System").assertIsDisplayed().assertHasClickAction()
        composeTestRule.onNodeWithText("Female Reproductive System").performClick()
        composeTestRule.runOnIdle { assertEquals(1, taps) }
    }

    @Test
    fun topicCard_showsProgressThroughVideosAndLessons() {
        var taps = 0
        composeTestRule.setContent {
            AfyaQuestTheme {
                TopicCard(
                    topic = LearnTopic.MATERNAL_HEALTH,
                    videosWatched = 2,
                    videosTotal = 7,
                    lessonsCompleted = 0,
                    lessonsTotal = 1,
                    onClick = { taps++ }
                )
            }
        }

        composeTestRule.onNodeWithText("Maternal Health").assertIsDisplayed()
        composeTestRule.onNodeWithText("2/7 videos • 0/1 lessons").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Completed").assertCountEquals(0)

        composeTestRule.onNodeWithText("Maternal Health").performClick()
        composeTestRule.runOnIdle { assertEquals(1, taps) }
    }

    @Test
    fun topicCard_showsDoneBadgeOnceEverythingIsFinished() {
        composeTestRule.setContent {
            AfyaQuestTheme {
                TopicCard(
                    topic = LearnTopic.NUTRITION,
                    videosWatched = 1,
                    videosTotal = 1,
                    lessonsCompleted = 1,
                    lessonsTotal = 1,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Nutrition").assertIsDisplayed()
        composeTestRule.onNodeWithText("1/1 videos • 1/1 lessons").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Completed").assertIsDisplayed()
    }

    @Test
    fun lessonMarkdown_showsFormattedTextWithoutMarkup() {
        composeTestRule.setContent {
            AfyaQuestTheme {
                LessonMarkdown(
                    content = "# Proper Handwashing\n\n## The 7 Steps\n\n1. **Wet hands** with clean water\n- Before eating"
                )
            }
        }

        // The opening title is dropped; headings and list items lose their markers.
        composeTestRule.onAllNodesWithText("Proper Handwashing", substring = true).assertCountEquals(0)
        composeTestRule.onNodeWithText("The 7 Steps").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wet hands with clean water").assertIsDisplayed()
        composeTestRule.onNodeWithText("Before eating").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("#", substring = true).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("**", substring = true).assertCountEquals(0)
    }
}
