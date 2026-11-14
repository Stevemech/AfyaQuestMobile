package com.afyaquest.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afyaquest.app.util.TokenManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * The whole app, started with somebody signed in. When the server ends the session, the app has
 * to go back to the sign-in screen and say why, instead of staying on a Home where every request
 * quietly fails.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SessionExpiryTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var tokenManager: TokenManager

    /** A session has to exist before the activity starts, so that it opens on Home. */
    @get:Rule(order = 1)
    val signedIn = object : ExternalResource() {
        override fun before() {
            hiltRule.inject()
            tokenManager.saveSession("access-token", "id-token", "refresh-token", "ui-test-user")
        }

        override fun after() {
            tokenManager.clearTokens()
        }
    }

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun appOpensStraightOnHomeWhenSignedIn() {
        // No splash screen in between: Home is the first screen.
        composeTestRule.onNodeWithText(text(R.string.total_xp)).assertIsDisplayed()
    }

    @Test
    fun whenTheServerEndsTheSession_theAppReturnsToLoginAndSaysWhy() {
        composeTestRule.onNodeWithText(text(R.string.total_xp)).assertIsDisplayed()

        tokenManager.expireSession()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text(R.string.sign_in)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(text(R.string.auth_session_expired)).assertIsDisplayed()
        // Nothing of the signed-in app is left on screen.
        assert(composeTestRule.onAllNodesWithText(text(R.string.total_xp)).fetchSemanticsNodes().isEmpty())
    }
}
