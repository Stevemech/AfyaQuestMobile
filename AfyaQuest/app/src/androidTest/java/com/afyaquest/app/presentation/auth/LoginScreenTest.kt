package com.afyaquest.app.presentation.auth

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
 * UI tests for the sign-in screen. Labels are read from the string resources, so the tests follow
 * the wording (and the language) of the app instead of a copy of it.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    private fun text(id: Int) = composeTestRule.activity.getString(id)

    @Before
    fun showLogin() {
        composeTestRule.setContent {
            AfyaQuestTheme {
                LoginScreen(navController = rememberNavController())
            }
        }
    }

    @Test
    fun loginScreen_displaysAllElements() {
        composeTestRule.onNodeWithText(text(R.string.welcome_back)).assertIsDisplayed()
        composeTestRule.onNodeWithText(text(R.string.sign_in_subtitle)).assertIsDisplayed()
        composeTestRule.onNodeWithText(text(R.string.email)).assertExists()
        composeTestRule.onNodeWithText(text(R.string.password)).assertExists()
        composeTestRule.onNodeWithText(text(R.string.sign_in)).assertExists()
        composeTestRule.onNodeWithText(text(R.string.forgot_password)).assertExists()
        composeTestRule.onNodeWithText(text(R.string.no_account), substring = true).assertExists()
        composeTestRule.onNodeWithText(text(R.string.sign_up)).assertHasClickAction()
    }

    @Test
    fun loginScreen_emailField_acceptsInput() {
        composeTestRule.onNodeWithText(text(R.string.email)).performTextInput("test@example.com")

        composeTestRule.onNodeWithText("test@example.com").assertExists()
    }

    @Test
    fun loginScreen_passwordIsHiddenUntilTheUserAsksToSeeIt() {
        composeTestRule.onNodeWithText(text(R.string.password)).performTextInput("password123")

        // Masked: the typed text is not on screen.
        composeTestRule.onAllNodesWithText("password123").fetchSemanticsNodes().let { nodes ->
            assert(nodes.isEmpty()) { "the password is shown in clear text" }
        }

        composeTestRule.onNodeWithContentDescription(text(R.string.show_password)).performClick()

        composeTestRule.onNodeWithText("password123").assertExists()
        composeTestRule.onNodeWithContentDescription(text(R.string.hide_password)).assertExists()
    }

    @Test
    fun loginScreen_signInButton_isClickable() {
        composeTestRule.onNodeWithText(text(R.string.email)).performTextInput("test@example.com")
        composeTestRule.onNodeWithText(text(R.string.password)).performTextInput("password123")

        composeTestRule.onNodeWithText(text(R.string.sign_in)).assertHasClickAction().assertIsEnabled()
    }
}
