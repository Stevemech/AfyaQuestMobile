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
        assertEquals(LanguageManager.LANGUAGE_SWAHILI, languageManager.getCurrentLanguageBlocking())
    }

    @Test
    fun `language constants are correct`() {
        assertEquals("en", LanguageManager.LANGUAGE_ENGLISH)
        assertEquals("sw", LanguageManager.LANGUAGE_SWAHILI)
    }

    @Test
    fun `per-user language is isolated`() = runBlocking {
        // User A sets Swahili
        languageManager.setCurrentUser("userA")
        languageManager.setLanguage(LanguageManager.LANGUAGE_SWAHILI)
        assertEquals(LanguageManager.LANGUAGE_SWAHILI, languageManager.getCurrentLanguage())

        // User B logs in – first time, inherits current language (Swahili from above)
        languageManager.setCurrentUser("userB")
        assertEquals(LanguageManager.LANGUAGE_SWAHILI, languageManager.getCurrentLanguage())

        // User B switches to English
        languageManager.setLanguage(LanguageManager.LANGUAGE_ENGLISH)
        assertEquals(LanguageManager.LANGUAGE_ENGLISH, languageManager.getCurrentLanguage())

        // Switch back to User A – should still be Swahili
        languageManager.setCurrentUser("userA")
        assertEquals(LanguageManager.LANGUAGE_SWAHILI, languageManager.getCurrentLanguage())
    }

    @Test
    fun `logout reverts to global preference`() = runBlocking {
        // Set global to English
        languageManager.setLanguage(LanguageManager.LANGUAGE_ENGLISH)

        // User logs in and sets Swahili
        languageManager.setCurrentUser("user1")
        languageManager.setLanguage(LanguageManager.LANGUAGE_SWAHILI)

        // Logout
        languageManager.setCurrentUser(null)

        // Global key should still reflect last active language (Swahili)
        assertEquals(LanguageManager.LANGUAGE_SWAHILI, languageManager.getCurrentLanguage())
    }
}
