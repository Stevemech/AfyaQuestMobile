package com.example.afyaquest

import android.os.Bundle
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
        setContent {
            AfyaQuestTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
