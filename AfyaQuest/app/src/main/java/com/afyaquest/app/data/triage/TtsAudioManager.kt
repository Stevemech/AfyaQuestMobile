package com.afyaquest.app.data.triage

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.afyaquest.app.domain.triage.AudioManifest
import com.afyaquest.app.domain.triage.AudioResolver
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** One spoken unit: a recorded clip (by [audioKey]) and/or its [text] for TTS fallback. */
data class SpeakItem(val audioKey: String, val text: String)

/**
 * Plays the "Tap to hear this step" audio: a sequence of utterances (a question
 * then each lettered option, or a disposition's label + instructions). Each unit
 * plays its recorded clip if one exists for the active language, otherwise falls
 * back to device text-to-speech — except Kaqchikel, which is never synthesized
 * (see [AudioResolver]).
 *
 * App-scoped singleton using the application context, so the bound TTS service
 * does not leak an Activity.
 */
@Singleton
class TtsAudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val resolver: AudioResolver by lazy { AudioResolver(loadManifest()) }

    private var tts: TextToSpeech? = null
    @Volatile private var ttsReady = false

    private var mediaPlayer: MediaPlayer? = null

    private data class Step(val clipPath: String?, val text: String)

    private var steps: List<Step> = emptyList()
    private var language: String = "en"
    /** Bumped on every stop()/speak() so stale playback callbacks are ignored. */
    private var token: Int = 0

    init {
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) = advance(utteranceId)
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = advance(utteranceId)
                    private fun advance(utteranceId: String?) {
                        val parts = utteranceId?.split(":") ?: return
                        val t = parts.getOrNull(0)?.toIntOrNull() ?: return
                        val idx = parts.getOrNull(1)?.toIntOrNull() ?: return
                        mainHandler.post { if (t == token) playFrom(idx + 1) }
                    }
                })
            }
        }
    }

    fun isAudioAvailable(language: String, audioKeys: List<String>): Boolean =
        resolver.isAudioAvailable(language, audioKeys)

    /** Speak [items] in order for [language]. Cancels anything already playing. */
    fun speak(items: List<SpeakItem>, language: String) {
        stop()
        this.language = language
        steps = items.map { Step(resolver.clipAssetPath(it.audioKey, language), it.text) }
        playFrom(0)
    }

    fun stop() {
        token++ // invalidate in-flight callbacks
        runCatching { tts?.stop() }
        mediaPlayer?.let { runCatching { it.stop() }; it.release() }
        mediaPlayer = null
        steps = emptyList()
    }

    fun shutdown() {
        stop()
        runCatching { tts?.shutdown() }
        tts = null
    }

    private fun playFrom(index: Int, initRetries: Int = TTS_INIT_RETRIES) {
        if (index >= steps.size) return
        val step = steps[index]
        when {
            step.clipPath != null -> playClip(step.clipPath, index)
            resolver.canSynthesize(language) && ttsReady -> speakTts(step.text, index)
            // TTS engine still binding (first utterance after app start) — wait for it
            // instead of silently dropping the step.
            resolver.canSynthesize(language) && initRetries > 0 -> {
                val myToken = token
                mainHandler.postDelayed({
                    if (myToken == token) playFrom(index, initRetries - 1)
                }, TTS_INIT_RETRY_MS)
            }
            else -> playFrom(index + 1) // Kaqchikel with no clip → skip, never synthesize
        }
    }

    private fun playClip(assetPath: String, index: Int) {
        val myToken = token
        try {
            val afd = context.assets.openFd(assetPath)
            val mp = MediaPlayer()
            mediaPlayer = mp
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setOnCompletionListener {
                mainHandler.post { if (myToken == token) playFrom(index + 1) }
            }
            mp.setOnErrorListener { _, _, _ ->
                mainHandler.post { if (myToken == token) playFrom(index + 1) }
                true
            }
            mp.prepare()
            mp.start()
        } catch (e: Exception) {
            if (myToken == token) playFrom(index + 1)
        }
    }

    private fun speakTts(text: String, index: Int) {
        val engine = tts ?: run { playFrom(index + 1); return }
        // setLanguage does its own availability check; on failure the engine keeps
        // its current voice, and speaking with the wrong accent beats silence.
        // (isLanguageAvailable pre-checks are unreliable on some engines.)
        engine.setLanguage(Locale(language))
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "$token:$index")
    }

    private fun loadManifest(): AudioManifest =
        try {
            val json = context.assets.open(MANIFEST_PATH).bufferedReader().use { it.readText() }
            Gson().fromJson(json, AudioManifest::class.java) ?: AudioManifest()
        } catch (e: Exception) {
            AudioManifest()
        }

    private companion object {
        const val MANIFEST_PATH = "audio/audio_manifest.json"

        /** How long to keep waiting for the TTS service to bind (10 × 300ms = 3s). */
        const val TTS_INIT_RETRIES = 10
        const val TTS_INIT_RETRY_MS = 300L
    }
}
