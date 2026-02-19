package com.example.afyaquest

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.afyaquest.presentation.navigation.NavGraph
import com.example.afyaquest.ui.theme.AfyaQuestTheme
import com.example.afyaquest.util.LanguageManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

/**
 * Main activity for the Afya Quest application.
 * Entry point for the app with Jetpack Compose UI and Navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var languageManager: LanguageManager

    override fun attachBaseContext(newBase: Context) {
        // Apply saved locale before the Activity context is created so all screens use it.
        val prefs = newBase.getSharedPreferences(LanguageManager.LANGUAGE_PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(LanguageManager.LANGUAGE_KEY, LanguageManager.LANGUAGE_ENGLISH) ?: LanguageManager.LANGUAGE_ENGLISH
        val locale = Locale(lang)
        val config = Configuration(newBase.resources.configuration).apply { setLocale(locale) }
        val wrapped = newBase.createConfigurationContext(config)
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep DataStore in sync and apply locale for resources
        languageManager.applySavedLanguageBlocking()
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
