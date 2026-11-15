package com.afyaquest.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.afyaquest.app.presentation.navigation.NavGraph
import com.afyaquest.app.presentation.navigation.Screen
import com.afyaquest.app.ui.theme.AfyaQuestTheme
import com.afyaquest.app.util.LanguageManager
import com.afyaquest.app.util.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

/**
 * Main activity for the AfyaQuest application.
 * Entry point for the app with Jetpack Compose UI and Navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var languageManager: LanguageManager

    @Inject
    lateinit var tokenManager: TokenManager

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

        // Restore per-user language context if already logged in.
        val userId = tokenManager.getUserId()
        val signedIn = tokenManager.isTokenValid() && userId != null
        if (signedIn) {
            languageManager.setCurrentUser(userId)
        } else {
            languageManager.applySavedLanguageBlocking()
        }
        // Known without touching the network or the encrypted token file, so the app opens
        // straight on the right screen (there used to be a fixed two-second splash here).
        val startDestination = if (signedIn) Screen.Dashboard.route else Screen.Login.route

        enableEdgeToEdge()

        // Handle deep link from email verification
        handleDeepLink(intent)

        setContent {
            AfyaQuestTheme {
                val navController = rememberNavController()

                // Observe language so all screens reactively get the correct locale.
                val initialLanguage = remember { languageManager.getCurrentLanguage() }
                val currentLanguage by languageManager.getCurrentLanguageFlow()
                    .collectAsState(initial = initialLanguage)

                // Use ContextThemeWrapper so Hilt can still find the Activity
                // in the context chain (createConfigurationContext returns a plain
                // ContextImpl which breaks hiltViewModel()).
                val activity = this@MainActivity
                val localizedContext = remember(currentLanguage) {
                    val locale = Locale(currentLanguage)
                    val overrideConfig = Configuration().apply { setLocale(locale) }
                    // Dialogs get a fresh ComposeView built on the Activity's own context, not
                    // this wrapper, so their text kept the language the app was opened in.
                    // Updating the Activity's resources makes dialogs follow the choice too.
                    val activityRes = activity.resources
                    @Suppress("DEPRECATION")
                    activityRes.updateConfiguration(
                        Configuration(activityRes.configuration).apply { setLocale(locale) },
                        activityRes.displayMetrics
                    )
                    ContextThemeWrapper(activity, 0).apply {
                        applyOverrideConfiguration(overrideConfig)
                    }
                }

                CompositionLocalProvider(LocalContext provides localizedContext) {
                    NavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        sessionExpired = tokenManager.sessionExpired
                    )
                }
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
            Toast.makeText(this, getString(R.string.auth_email_verified), Toast.LENGTH_LONG).show()
            // Handled: without this the message came back after every rotation.
            intent.data = null
        }
    }
}
