package com.example.afyaquest.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app language/locale settings
 */
@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_SWAHILI = "sw"

        /** Same name as DataStore; also used so MainActivity.attachBaseContext can read locale before DI. */
        const val LANGUAGE_PREFS_NAME = "language_prefs"
        const val LANGUAGE_KEY = "selected_language"

        private val Context.languageDataStore by preferencesDataStore(name = LANGUAGE_PREFS_NAME)
        private val LANGUAGE_KEY_DS = stringPreferencesKey(LANGUAGE_KEY)
    }

    /**
     * Get current language as Flow
     */
    fun getCurrentLanguageFlow(): Flow<String> {
        return context.languageDataStore.data.map { preferences ->
            preferences[LANGUAGE_KEY_DS] ?: LANGUAGE_ENGLISH
        }
    }

    /**
     * Get current language synchronously from SharedPreferences.
     */
    fun getCurrentLanguageBlocking(): String {
        return context.getSharedPreferences(LANGUAGE_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(LANGUAGE_KEY, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }

    /**
     * Get current language from default locale.
     */
    fun getCurrentLanguage(): String {
        return Locale.getDefault().language.let { lang ->
            when (lang) {
                LANGUAGE_SWAHILI -> LANGUAGE_SWAHILI
                else -> LANGUAGE_ENGLISH
            }
        }
    }

    /**
     * Set app language
     */
    suspend fun setLanguage(languageCode: String) {
        // Save to DataStore
        context.languageDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY_DS] = languageCode
        }

        // Save to SharedPreferences synchronously so attachBaseContext (after recreate) sees it.
        context.getSharedPreferences(LANGUAGE_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_KEY, languageCode)
            .commit()

        // Apply to app
        applyLanguage(languageCode)
    }

    /**
     * Apply saved language at app/activity start.
     */
    fun applySavedLanguageBlocking() {
        runBlocking {
            val lang = context.languageDataStore.data.first()[LANGUAGE_KEY_DS] ?: LANGUAGE_ENGLISH
            // Keep SharedPreferences in sync for attachBaseContext
            context.getSharedPreferences(LANGUAGE_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(LANGUAGE_KEY, lang)
                .commit()
            applyLanguage(lang)
        }
    }

    /**
     * Apply language to context
     */
    private fun applyLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        }

        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    /**
     * Get display name for language
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            LANGUAGE_ENGLISH -> "English"
            LANGUAGE_SWAHILI -> "Kiswahili"
            else -> "English"
        }
    }

    /**
     * Get all available languages
     */
    fun getAvailableLanguages(): List<Pair<String, String>> {
        return listOf(
            LANGUAGE_ENGLISH to "English",
            LANGUAGE_SWAHILI to "Kiswahili"
        )
    }
}
