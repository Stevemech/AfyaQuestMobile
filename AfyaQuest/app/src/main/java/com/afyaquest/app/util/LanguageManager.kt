package com.afyaquest.app.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app language/locale settings on a per-user basis.
 *
 * Language preferences are stored in SharedPreferences keyed by user ID so each
 * user gets their own language setting. A global key is kept in sync for
 * [android.app.Activity.attachBaseContext] which runs before Hilt injection.
 */
@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_SPANISH = "es"
        const val LANGUAGE_KAQCHIKEL = "cak"
        const val LANGUAGE_SWAHILI = "sw"

        /** SharedPreferences file name - also read by MainActivity.attachBaseContext. */
        const val LANGUAGE_PREFS_NAME = "language_prefs"

        /** Global key used by attachBaseContext (always reflects the active language). */
        const val LANGUAGE_KEY = "selected_language"

        private const val USER_LANG_PREFIX = "lang_"
    }

    /** The currently logged-in user's ID, or null when on the login screen. */
    private var currentUserId: String? = null

    private val _currentLanguage = MutableStateFlow(readGlobalLanguage())

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Switch to per-user language preferences.
     *
     * Call with `null` on logout to revert to the global (login-screen) pref.
     *
     * @param inheritScreenLanguage Pass `true` when called right after a
     *   successful login so the language chosen on the login screen carries
     *   over. On app-restart (no login screen shown) leave as `false` to
     *   restore the user's saved preference.
     */
    fun setCurrentUser(userId: String?, inheritScreenLanguage: Boolean = false) {
        currentUserId = userId

        if (userId == null) {
            // Logout – the global key already reflects the last active language.
            return
        }

        val prefs = getPrefs()
        val userKey = userLangKey(userId)

        val lang = if (inheritScreenLanguage) {
            // Login screen → adopt whatever language is currently shown.
            _currentLanguage.value
        } else {
            // App restart → restore per-user preference, falling back to current.
            prefs.getString(userKey, null) ?: _currentLanguage.value
        }

        // Keep the global key in sync for attachBaseContext after process death.
        saveLanguage(prefs, lang, userKey, LANGUAGE_KEY)

        _currentLanguage.value = lang
        applyLanguage(lang)
    }

    /**
     * Store [lang] under [keys] in one write, and only when a value really changes. This runs on
     * the main thread at every start of the app; it used to be two blocking disk writes each
     * time, even when nothing had changed.
     */
    private fun saveLanguage(prefs: SharedPreferences, lang: String, vararg keys: String) {
        val stale = keys.filter { prefs.getString(it, null) != lang }
        if (stale.isEmpty()) return
        val editor = prefs.edit()
        stale.forEach { editor.putString(it, lang) }
        editor.apply()
    }

    /**
     * Observe the current language reactively (for Compose StateFlows).
     */
    fun getCurrentLanguageFlow(): Flow<String> = _currentLanguage

    /**
     * Synchronous read – safe to call from non-coroutine contexts.
     */
    fun getCurrentLanguageBlocking(): String {
        return getPrefs().getString(activeKey(), LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }

    /**
     * Convenience accessor backed by the StateFlow.
     */
    fun getCurrentLanguage(): String = _currentLanguage.value

    /**
     * Persist a new language choice and apply it immediately.
     */
    suspend fun setLanguage(languageCode: String) {
        // The user-specific key (or the global one when nobody is signed in), mirrored to the
        // global key for attachBaseContext.
        saveLanguage(getPrefs(), languageCode, activeKey(), LANGUAGE_KEY)

        _currentLanguage.value = languageCode
        applyLanguage(languageCode)
    }

    /**
     * Called once in [MainActivity.onCreate] to ensure the locale is applied
     * after the Activity is fully created. Reads from the active key (user or
     * global) and syncs the global key.
     */
    fun applySavedLanguageBlocking() {
        val lang = getPrefs().getString(activeKey(), LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
        saveLanguage(getPrefs(), lang, LANGUAGE_KEY)
        _currentLanguage.value = lang
        applyLanguage(lang)
    }

    // ── Display helpers ─────────────────────────────────────────────────

    fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            LANGUAGE_ENGLISH -> "English"
            LANGUAGE_SPANISH -> "Español"
            LANGUAGE_KAQCHIKEL -> "Kaqchikel"
            LANGUAGE_SWAHILI -> "Kiswahili"
            else -> "English"
        }
    }

    fun getAvailableLanguages(): List<Pair<String, String>> {
        return listOf(
            LANGUAGE_ENGLISH to "English",
            LANGUAGE_SPANISH to "Español",
            LANGUAGE_KAQCHIKEL to "Kaqchikel",
            LANGUAGE_SWAHILI to "Kiswahili"
        )
    }

    // ── Internals ───────────────────────────────────────────────────────

    private fun getPrefs(): SharedPreferences {
        return context.getSharedPreferences(LANGUAGE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun userLangKey(userId: String): String = "$USER_LANG_PREFIX$userId"

    /** Returns the SharedPreferences key for the current context (user or global). */
    private fun activeKey(): String {
        val uid = currentUserId
        return if (uid != null) userLangKey(uid) else LANGUAGE_KEY
    }

    private fun readGlobalLanguage(): String {
        return context.getSharedPreferences(LANGUAGE_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(LANGUAGE_KEY, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }

    private fun applyLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        // ViewModels read strings through the application context, so its resources follow the
        // chosen language too.
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
