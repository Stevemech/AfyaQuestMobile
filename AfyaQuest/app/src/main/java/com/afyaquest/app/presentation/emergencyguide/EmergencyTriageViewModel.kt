package com.afyaquest.app.presentation.emergencyguide

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afyaquest.app.data.triage.CaseLogRepository
import com.afyaquest.app.data.triage.SpeakItem
import com.afyaquest.app.data.triage.TriageTreeRepository
import com.afyaquest.app.data.triage.TtsAudioManager
import com.afyaquest.app.domain.triage.TriageEngine
import com.afyaquest.app.domain.triage.TriageState
import com.afyaquest.app.domain.triage.TriageTree
import com.afyaquest.app.domain.triage.localized
import com.afyaquest.app.util.LanguageManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Drives one emergency triage assessment over the data-driven [TriageEngine].
 *
 * The traversal is held as an immutable [TriageState]; the answer path is mirrored
 * into [SavedStateHandle] and replayed on restore, so progress survives process
 * death and a mid-flow language switch (which only changes which localized text is
 * shown — the engine state is language-independent).
 */
@HiltViewModel
class EmergencyTriageViewModel @Inject constructor(
    repository: TriageTreeRepository,
    private val caseLogRepository: CaseLogRepository,
    private val audioManager: TtsAudioManager,
    private val languageManager: LanguageManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val engine: TriageEngine = repository.engine()
    val tree: TriageTree get() = engine.tree

    private val gson = Gson()

    /** Stable id for this assessment session; case logs REPLACE on it (idempotent). */
    private var sessionId: String = savedStateHandle.get<String>(KEY_SESSION) ?: newSession()

    private val _state = MutableStateFlow(restoreState())
    val state: StateFlow<TriageState> = _state.asStateFlow()

    /** Active language; toggling re-localizes the UI without losing place. */
    val currentLanguage: StateFlow<String> = languageManager.getCurrentLanguageFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, languageManager.getCurrentLanguage())

    fun answer(letter: String) {
        val current = _state.value
        if (current.isDisposition) return
        audioManager.stop()
        val next = engine.answer(current, letter)
        _state.value = next
        persist()
        if (next.isDisposition) logCase(next)
    }

    /** Step back to the previous question by replaying the path minus the last answer. */
    fun back() {
        val current = _state.value
        if (current.path.isEmpty()) return
        audioManager.stop()
        var rebuilt = engine.start()
        for (step in current.path.dropLast(1)) rebuilt = engine.answer(rebuilt, step.optionLetter)
        _state.value = rebuilt
        persist()
    }

    fun restart() {
        audioManager.stop()
        sessionId = newSession()
        _state.value = engine.start()
        persist()
    }

    fun setLanguage(code: String) {
        audioManager.stop()
        viewModelScope.launch { languageManager.setLanguage(code) }
    }

    // ── Voice ("Tap to hear this step") ─────────────────────────────────

    /** Whether the current step can be spoken in the active language. */
    fun isAudioAvailableForCurrentStep(): Boolean =
        audioManager.isAudioAvailable(currentLanguage.value, currentStepAudioKeys())

    /** Read the current question + its options aloud, or the disposition guidance. */
    fun speakCurrentStep() {
        audioManager.speak(currentStepSpeakItems(), currentLanguage.value)
    }

    fun stopSpeaking() = audioManager.stop()

    override fun onCleared() {
        audioManager.stop()
        super.onCleared()
    }

    private fun currentStepSpeakItems(): List<SpeakItem> {
        val lang = currentLanguage.value
        val s = _state.value
        if (s.isDisposition) {
            val d = engine.disposition(s.currentId) ?: return emptyList()
            val key = d.audioKey ?: d.id
            return listOf(
                SpeakItem(key, d.label.localized(lang)),
                SpeakItem("${key}_instructions", d.instructions.localized(lang))
            )
        }
        val node = engine.node(s.currentId) ?: return emptyList()
        return buildList {
            add(SpeakItem(node.audioKey ?: node.id, node.text.localized(lang)))
            node.options?.forEach { opt ->
                add(SpeakItem(opt.audioKey ?: "${node.id}_${opt.letter}", "${opt.letter}. ${opt.label.localized(lang)}"))
            }
        }
    }

    private fun currentStepAudioKeys(): List<String> = currentStepSpeakItems().map { it.audioKey }

    private fun logCase(disposition: TriageState) {
        val level = engine.disposition(disposition.currentId)?.level ?: ""
        viewModelScope.launch {
            caseLogRepository.logDisposition(
                caseId = sessionId,
                state = disposition,
                dispositionLevel = level,
                treeVersion = tree.version,
                language = currentLanguage.value
            )
        }
    }

    private fun newSession(): String =
        UUID.randomUUID().toString().also { savedStateHandle[KEY_SESSION] = it }

    private fun persist() {
        savedStateHandle[KEY_PATH] = gson.toJson(_state.value.path.map { it.optionLetter })
    }

    private fun restoreState(): TriageState {
        val json = savedStateHandle.get<String>(KEY_PATH) ?: return engine.start()
        return try {
            val letters: List<String> =
                gson.fromJson(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
            var s = engine.start()
            for (letter in letters) {
                if (s.isDisposition) break
                s = engine.answer(s, letter)
            }
            s
        } catch (e: Exception) {
            engine.start()
        }
    }

    private companion object {
        const val KEY_PATH = "triage_path"
        const val KEY_SESSION = "triage_session"
    }
}
