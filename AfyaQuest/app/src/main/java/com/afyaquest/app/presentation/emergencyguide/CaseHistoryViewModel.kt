package com.afyaquest.app.presentation.emergencyguide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afyaquest.app.data.local.entity.CaseLogEntity
import com.afyaquest.app.data.triage.CaseLogRepository
import com.afyaquest.app.data.triage.TriageTreeRepository
import com.afyaquest.app.domain.triage.TriageEngine
import com.afyaquest.app.util.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CaseHistoryViewModel @Inject constructor(
    caseLogRepository: CaseLogRepository,
    treeRepository: TriageTreeRepository,
    languageManager: LanguageManager
) : ViewModel() {

    /** Used to resolve a logged disposition id back to its localized label + color. */
    val engine: TriageEngine = treeRepository.engine()

    val cases: StateFlow<List<CaseLogEntity>> = caseLogRepository.observeCurrentUserCases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentLanguage: StateFlow<String> = languageManager.getCurrentLanguageFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, languageManager.getCurrentLanguage())
}
