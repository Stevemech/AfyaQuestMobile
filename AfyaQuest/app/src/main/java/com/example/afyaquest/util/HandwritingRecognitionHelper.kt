package com.example.afyaquest.util

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Thin wrapper around ML Kit Digital Ink Recognition.
 *
 * Downloads the language model on first use (~2 MB for Latin script) and
 * provides a suspend [recognize] function that converts [Ink] strokes to text.
 */
class HandwritingRecognitionHelper {

    private var recognizer: DigitalInkRecognizer? = null
    private var currentModelTag: String? = null

    /**
     * Ensures the model for [languageTag] is downloaded and a recognizer is ready.
     *
     * @param languageTag BCP-47 language tag, e.g. "en-US" or "sw-TZ".
     * @return `true` if the model is ready, `false` if download/init failed.
     */
    suspend fun ensureModelReady(languageTag: String): Boolean {
        if (currentModelTag == languageTag && recognizer != null) return true

        val identifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
            ?: return false

        val model = DigitalInkRecognitionModel.builder(identifier).build()

        // Download if needed
        val downloaded = suspendCancellableCoroutine { cont ->
            RemoteModelManager.getInstance()
                .download(model, DownloadConditions.Builder().build())
                .addOnSuccessListener { cont.resume(true) }
                .addOnFailureListener { cont.resume(false) }
        }

        if (!downloaded) return false

        // Close previous recognizer if switching languages
        recognizer?.close()

        recognizer = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).build()
        )
        currentModelTag = languageTag

        return true
    }

    /**
     * Runs recognition on the given [ink] strokes.
     *
     * @return The best recognition candidate, or empty string if nothing was recognized.
     * @throws Exception if no recognizer is ready (call [ensureModelReady] first).
     */
    suspend fun recognize(ink: Ink): String {
        val rec = recognizer
            ?: throw IllegalStateException("Call ensureModelReady() before recognize()")

        return suspendCancellableCoroutine { cont ->
            rec.recognize(ink)
                .addOnSuccessListener { result ->
                    val best = result.candidates.firstOrNull()?.text.orEmpty()
                    cont.resume(best)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }

    fun close() {
        recognizer?.close()
        recognizer = null
        currentModelTag = null
    }
}
