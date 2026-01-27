package com.example.afyaquest

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.afyaquest.presentation.navigation.NavGraph
import com.example.afyaquest.ui.theme.AfyaQuestTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the Afya Quest application.
 * Entry point for the app with Jetpack Compose UI and Navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle deep link from email verification
        handleDeepLink(intent)

        setContent {
            AfyaQuestTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data
        if (data?.scheme == "afyaquest" && data.host == "verified") {
            // User clicked email verification link
            Toast.makeText(
                this,
                "Email verified! You can now log in.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
