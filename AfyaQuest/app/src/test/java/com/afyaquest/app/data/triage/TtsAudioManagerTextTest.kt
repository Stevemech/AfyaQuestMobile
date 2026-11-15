package com.afyaquest.app.data.triage

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class TtsAudioManagerTextTest {

    @Test
    fun long_capitalised_words_are_spoken_in_lower_case() {
        assertEquals(
            "Mpeleke kituo cha afya sasa. Mshughulikie kama mara moja.",
            TtsAudioManager.spokenText("Mpeleke kituo cha afya SASA. Mshughulikie kama MARA MOJA.")
        )
    }

    @Test
    fun short_acronyms_are_left_alone() {
        assertEquals("Start CPR, use the AED. Is it safe for YOU?",
            TtsAudioManager.spokenText("Start CPR, use the AED. Is it safe for YOU?"))
    }

    @Test
    fun swahili_prefers_kenyan_then_tanzanian_voice() {
        assertEquals(
            listOf("sw-KE", "sw-TZ", "sw"),
            TtsAudioManager.voiceCandidates("sw").map { it.toLanguageTag() }
        )
    }

    @Test
    fun unknown_language_falls_back_to_its_own_tag() {
        assertEquals(listOf(Locale.forLanguageTag("fr")), TtsAudioManager.voiceCandidates("fr"))
    }
}
