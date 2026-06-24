package com.afyaquest.app.presentation.emergencyguide

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afyaquest.app.data.triage.TriageTreeRepository
import com.afyaquest.app.domain.triage.TriageEngine
import com.afyaquest.app.domain.triage.TriageState
import com.afyaquest.app.domain.triage.TriageTree
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
    private val languageManager: LanguageManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val engine: TriageEngine = repository.engine()
    val tree: TriageTree get() = engine.tree

    private val gson = Gson()

    private val _state = MutableStateFlow(restoreState())
    val state: StateFlow<TriageState> = _state.asStateFlow()

    /** Active language; toggling re-localizes the UI without losing place. */
    val currentLanguage: StateFlow<String> = languageManager.getCurrentLanguageFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, languageManager.getCurrentLanguage())

    fun answer(letter: String) {
        val current = _state.value
        if (current.isDisposition) return
        _state.value = engine.answer(current, letter)
        persist()
    }

    /** Step back to the previous question by replaying the path minus the last answer. */
    fun back() {
        val current = _state.value
        if (current.path.isEmpty()) return
        var rebuilt = engine.start()
        for (step in current.path.dropLast(1)) rebuilt = engine.answer(rebuilt, step.optionLetter)
        _state.value = rebuilt
        persist()
    }

    fun restart() {
        _state.value = engine.start()
        persist()
    }

    fun setLanguage(code: String) {
        viewModelScope.launch { languageManager.setLanguage(code) }
    }

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
    }
}
