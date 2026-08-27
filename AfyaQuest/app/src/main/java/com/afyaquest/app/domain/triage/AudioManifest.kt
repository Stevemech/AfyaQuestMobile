package com.afyaquest.app.domain.triage

/**
 * Lists which pre-recorded audio clips exist, per language. Loaded from
 * `assets/audio/audio_manifest.json`. Clips live at
 * `assets/audio/<language>/<audioKey>.mp3`.
 *
 * Empty until native-speaker recordings are produced (see docs/emergency-response/AUDIO.md).
 */
data class AudioManifest(
    val version: String = "0",
    /** language code -> list of audioKeys that have a recorded clip. */
    val clips: Map<String, List<String>> = emptyMap()
)

/**
 * Pure resolution logic for the voice layer — kept Android-free so the critical
 * rule (NEVER synthesize Kaqchikel) is unit-testable.
 *
 * Resolution order for an utterance: a recorded clip for the active language if
 * one exists, otherwise device text-to-speech — but only for languages a TTS
 * engine can faithfully speak. Kaqchikel is never synthesized (no engine supports
 * it; faking it through Spanish phonemes mangles the orthography).
 */
class AudioResolver(private val manifest: AudioManifest) {

    fun hasClip(audioKey: String, language: String): Boolean =
        manifest.clips[language]?.contains(audioKey) == true

    fun clipAssetPath(audioKey: String, language: String): String? =
        if (hasClip(audioKey, language)) "$AUDIO_DIR/$language/$audioKey$CLIP_EXT" else null

    /** No mainstream TTS engine supports Kaqchikel — never synthesize it. */
    fun canSynthesize(language: String): Boolean = language != LANG_KAQCHIKEL

    /** Whether any audio (a clip or synthesis) can be produced for this step. */
    fun isAudioAvailable(language: String, audioKeys: List<String>): Boolean =
        canSynthesize(language) || audioKeys.any { hasClip(it, language) }

    companion object {
        const val LANG_KAQCHIKEL = "cak"
        const val AUDIO_DIR = "audio"
        const val CLIP_EXT = ".mp3"
    }
}
