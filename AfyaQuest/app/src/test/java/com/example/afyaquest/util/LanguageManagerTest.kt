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
        assertEquals("Español", languageManager.getLanguageDisplayName(LanguageManager.LANGUAGE_SPANISH))
        assertEquals("Kaqchikel", languageManager.getLanguageDisplayName(LanguageManager.LANGUAGE_KAQCHIKEL))
        assertEquals("English", languageManager.getLanguageDisplayName("unknown"))
    }

    @Test
    fun `getAvailableLanguages returns all languages`() {
        val languages = languageManager.getAvailableLanguages()

        assertEquals(3, languages.size)
        assertTrue(languages.contains(LanguageManager.LANGUAGE_ENGLISH to "English"))
        assertTrue(languages.contains(LanguageManager.LANGUAGE_SPANISH to "Español"))
        assertTrue(languages.contains(LanguageManager.LANGUAGE_KAQCHIKEL to "Kaqchikel"))
    }

    @Test
    fun `setLanguage changes language preference`() = runBlocking {
        languageManager.setLanguage(LanguageManager.LANGUAGE_SPANISH)
        assertEquals(LanguageManager.LANGUAGE_SPANISH, languageManager.getCurrentLanguageBlocking())

        languageManager.setLanguage(LanguageManager.LANGUAGE_KAQCHIKEL)
        assertEquals(LanguageManager.LANGUAGE_KAQCHIKEL, languageManager.getCurrentLanguageBlocking())
    }

    @Test
    fun `language constants are correct`() {
        assertEquals("en", LanguageManager.LANGUAGE_ENGLISH)
        assertEquals("es", LanguageManager.LANGUAGE_SPANISH)
        assertEquals("cak", LanguageManager.LANGUAGE_KAQCHIKEL)
    }

    @Test
    fun `per-user language is isolated`() = runBlocking {
        // User A sets Kaqchikel
        languageManager.setCurrentUser("userA")
        languageManager.setLanguage(LanguageManager.LANGUAGE_KAQCHIKEL)
        assertEquals(LanguageManager.LANGUAGE_KAQCHIKEL, languageManager.getCurrentLanguage())

        // User B logs in – first time, inherits current language (Kaqchikel from above)
        languageManager.setCurrentUser("userB")
        assertEquals(LanguageManager.LANGUAGE_KAQCHIKEL, languageManager.getCurrentLanguage())

        // User B switches to Spanish
        languageManager.setLanguage(LanguageManager.LANGUAGE_SPANISH)
        assertEquals(LanguageManager.LANGUAGE_SPANISH, languageManager.getCurrentLanguage())

        // Switch back to User A – should still be Kaqchikel
        languageManager.setCurrentUser("userA")
        assertEquals(LanguageManager.LANGUAGE_KAQCHIKEL, languageManager.getCurrentLanguage())
    }

    @Test
    fun `logout reverts to global preference`() = runBlocking {
        // Set global to English
        languageManager.setLanguage(LanguageManager.LANGUAGE_ENGLISH)

        // User logs in and sets Spanish
        languageManager.setCurrentUser("user1")
        languageManager.setLanguage(LanguageManager.LANGUAGE_SPANISH)

        // Logout
        languageManager.setCurrentUser(null)

        // Global key should still reflect last active language (Spanish)
        assertEquals(LanguageManager.LANGUAGE_SPANISH, languageManager.getCurrentLanguage())
    }
}
