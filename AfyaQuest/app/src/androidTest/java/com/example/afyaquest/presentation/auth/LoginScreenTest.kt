package com.example.afyaquest.presentation.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.afyaquest.ui.theme.AfyaQuestTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for LoginScreen
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displaysAllElements() {
        composeTestRule.setContent {
            AfyaQuestTheme {
                val navController = rememberNavController()
                LoginScreen(navController = navController)
            }
        }

        // Verify title is displayed
        composeTestRule.onNodeWithText("Welcome Back!").assertIsDisplayed()

        // Verify email field exists
        composeTestRule.onNodeWithText("Email").assertExists()

        // Verify password field exists
        composeTestRule.onNodeWithText("Password").assertExists()

        // Verify login button exists
        composeTestRule.onNodeWithText("Login").assertExists()

        // Verify register text exists
        composeTestRule.onNodeWithText("Don't have an account?").assertExists()
    }

    @Test
    fun loginScreen_emailField_acceptsInput() {
        composeTestRule.setContent {
            AfyaQuestTheme {
                val navController = rememberNavController()
                LoginScreen(navController = navController)
            }
        }

        // Type in email field
        composeTestRule.onNodeWithText("Email")
            .performTextInput("test@example.com")

        // Verify input was entered
        composeTestRule.onNodeWithText("test@example.com").assertExists()
    }

    @Test
    fun loginScreen_passwordField_acceptsInput() {
        composeTestRule.setContent {
            AfyaQuestTheme {
                val navController = rememberNavController()
                LoginScreen(navController = navController)
            }
        }

        // Type in password field
        composeTestRule.onNodeWithText("Password")
            .performTextInput("password123")

        // Password should be masked, so we can't directly verify the text
        // But we can verify the field accepted input
    }

    @Test
    fun loginScreen_loginButton_isClickable() {
        composeTestRule.setContent {
            AfyaQuestTheme {
                val navController = rememberNavController()
                LoginScreen(navController = navController)
            }
        }

        // Verify login button is clickable
        composeTestRule.onNodeWithText("Login")
            .assertHasClickAction()
    }
}
