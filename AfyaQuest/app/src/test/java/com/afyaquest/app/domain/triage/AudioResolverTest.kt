package com.afyaquest.app.domain.triage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks in the voice-layer resolution rules — most importantly that Kaqchikel is
 * NEVER synthesized and only ever plays from a real recorded clip.
 */
class AudioResolverTest {

    private val emptyManifest = AudioManifest()
    private val esClips = AudioManifest(clips = mapOf("es" to listOf("scene_safe", "scene_safe_a")))
    private val cakClips = AudioManifest(clips = mapOf("cak" to listOf("scene_safe")))

    @Test
    fun english_and_spanish_can_synthesize_even_with_no_clips() {
        val r = AudioResolver(emptyManifest)
        assertTrue(r.canSynthesize("en"))
        assertTrue(r.canSynthesize("es"))
        assertTrue(r.isAudioAvailable("en", listOf("scene_safe")))
        assertTrue(r.isAudioAvailable("es", listOf("scene_safe")))
    }

    @Test
    fun kaqchikel_is_never_synthesized() {
        val r = AudioResolver(emptyManifest)
        assertFalse(r.canSynthesize("cak"))
        // No clips -> no audio at all for Kaqchikel (text-only, never a fake voice).
        assertFalse(r.isAudioAvailable("cak", listOf("scene_safe", "scene_safe_a")))
        assertNull(r.clipAssetPath("scene_safe", "cak"))
    }

    @Test
    fun kaqchikel_with_a_recorded_clip_becomes_available() {
        val r = AudioResolver(cakClips)
        assertFalse(r.canSynthesize("cak")) // still never synthesized
        assertTrue(r.isAudioAvailable("cak", listOf("scene_safe")))
        assertEquals("audio/cak/scene_safe.mp3", r.clipAssetPath("scene_safe", "cak"))
        // A key without a clip stays unavailable for cak.
        assertNull(r.clipAssetPath("scene_ppe", "cak"))
        assertFalse(r.isAudioAvailable("cak", listOf("scene_ppe")))
    }

    @Test
    fun clip_path_is_resolved_only_for_listed_keys() {
        val r = AudioResolver(esClips)
        assertEquals("audio/es/scene_safe.mp3", r.clipAssetPath("scene_safe", "es"))
        assertEquals("audio/es/scene_safe_a.mp3", r.clipAssetPath("scene_safe_a", "es"))
        assertNull(r.clipAssetPath("prim_breathing", "es"))
    }
}
