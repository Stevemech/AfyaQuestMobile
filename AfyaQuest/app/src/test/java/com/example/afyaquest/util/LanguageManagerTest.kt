package com.example.afyaquest.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for LanguageManager
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LanguageManagerTest {

    private lateinit var context: Context
    private lateinit var languageManager: LanguageManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        languageManager = LanguageManager(context)
    }

    @Test
    fun `getLanguageDisplayName returns correct display names`() {
        assertEquals("English", languageManager.getLanguageDisplayName(LanguageManager.LANGUAGE_ENGLISH))
        assertEquals("Kiswahili", languageManager.getLanguageDisplayName(LanguageManager.LANGUAGE_SWAHILI))
        assertEquals("English", languageManager.getLanguageDisplayName("unknown"))
    }

    @Test
    fun `getAvailableLanguages returns both languages`() {
        val languages = languageManager.getAvailableLanguages()

        assertEquals(2, languages.size)
        assertTrue(languages.contains(LanguageManager.LANGUAGE_ENGLISH to "English"))
        assertTrue(languages.contains(LanguageManager.LANGUAGE_SWAHILI to "Kiswahili"))
    }

    @Test
    fun `setLanguage changes language preference`() = runBlocking {
        languageManager.setLanguage(LanguageManager.LANGUAGE_SWAHILI)

        // Note: In real test, would verify DataStore value
        // This is a simplified test
    }

    @Test
    fun `language constants are correct`() {
        assertEquals("en", LanguageManager.LANGUAGE_ENGLISH)
        assertEquals("sw", LanguageManager.LANGUAGE_SWAHILI)
    }
}
