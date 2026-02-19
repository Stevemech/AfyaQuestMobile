package com.example.afyaquest.presentation.report

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afyaquest.data.repository.ReportsRepository
import com.example.afyaquest.domain.model.DailyReport
import com.example.afyaquest.util.LanguageManager
import com.example.afyaquest.util.Resource
import com.example.afyaquest.util.XpManager
import com.example.afyaquest.util.XpRewards
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.afyaquest.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for Daily Report screen
 */
@HiltViewModel
class DailyReportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val xpManager: XpManager,
    private val reportsRepository: ReportsRepository,
    private val languageManager: LanguageManager
) : ViewModel() {

    private val _patientsVisited = MutableStateFlow("")
    val patientsVisited: StateFlow<String> = _patientsVisited.asStateFlow()

    private val _vaccinationsGiven = MutableStateFlow("")
    val vaccinationsGiven: StateFlow<String> = _vaccinationsGiven.asStateFlow()

    private val _healthEducation = MutableStateFlow("")
    val healthEducation: StateFlow<String> = _healthEducation.asStateFlow()

    private val _challenges = MutableStateFlow("")
    val challenges: StateFlow<String> = _challenges.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _submissionState = MutableStateFlow<Resource<String>?>(null)
    val submissionState: StateFlow<Resource<String>?> = _submissionState.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _reportHistory = MutableStateFlow<List<DailyReport>>(emptyList())
    val reportHistory: StateFlow<List<DailyReport>> = _reportHistory.asStateFlow()

    private val _historyLoading = MutableStateFlow(false)
    val historyLoading: StateFlow<Boolean> = _historyLoading.asStateFlow()

    fun getCurrentLanguage(): String = languageManager.getCurrentLanguage()

    val healthEducationTopics: List<String> get() = listOf(
        context.getString(R.string.category_hygiene),
        context.getString(R.string.category_nutrition),
        context.getString(R.string.category_disease_prevention),
        context.getString(R.string.category_maternal_health),
        context.getString(R.string.category_child_care)
    )

    init {
        loadReportHistory()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun loadReportHistory() {
        viewModelScope.launch {
            _historyLoading.value = true
            reportsRepository.getReportsForCurrentUser().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _reportHistory.value = resource.data ?: emptyList()
                        _historyLoading.value = false
                    }
                    is Resource.Error -> {
                        _historyLoading.value = false
                    }
                    is Resource.Loading -> {
                        _historyLoading.value = true
                    }
                }
            }
        }
    }

    fun setPatientsVisited(value: String) {
        // Only allow non-negative numbers
        if (value.isEmpty() || value.toIntOrNull()?.let { it >= 0 } == true) {
            _patientsVisited.value = value
        }
    }

    fun setVaccinationsGiven(value: String) {
        // Only allow non-negative numbers
        if (value.isEmpty() || value.toIntOrNull()?.let { it >= 0 } == true) {
            _vaccinationsGiven.value = value
        }
    }

    fun setHealthEducation(value: String) {
        _healthEducation.value = value
    }

    fun setChallenges(value: String) {
        _challenges.value = value
    }

    fun setNotes(value: String) {
        _notes.value = value
    }

    /**
     * Validate form fields
     */
    fun isFormValid(): Boolean {
        return _patientsVisited.value.isNotEmpty() &&
                _vaccinationsGiven.value.isNotEmpty() &&
                _healthEducation.value.isNotEmpty()
    }

    /**
     * Submit daily report
     */
    fun submitReport() {
        if (!isFormValid()) {
            _submissionState.value = Resource.Error(context.getString(R.string.fill_required_fields))
            return
        }

        viewModelScope.launch {
            _submissionState.value = Resource.Loading()

            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                timestamp.timeZone = TimeZone.getTimeZone("UTC")

                val report = DailyReport(
                    id = System.currentTimeMillis().toString(),
                    date = dateFormat.format(Date()),
                    timestamp = timestamp.format(Date()),
                    patientsVisited = _patientsVisited.value.toInt(),
                    vaccinationsGiven = _vaccinationsGiven.value.toInt(),
                    healthEducation = _healthEducation.value,
                    challenges = _challenges.value,
                    notes = _notes.value
                )

                reportsRepository.saveReport(report).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            // Award XP for submitting daily report
                            xpManager.addXP(
                                XpRewards.DAILY_REPORT,
                                "Submitted daily report"
                            )
                            _submissionState.value = Resource.Success(
                                context.getString(R.string.report_submitted_success)
                            )
                            resetForm()
                        }
                        is Resource.Error -> {
                            _submissionState.value = Resource.Error(
                                resource.message ?: context.getString(R.string.submission_failed)
                            )
                        }
                        is Resource.Loading -> {
                            _submissionState.value = Resource.Loading()
                        }
                    }
                }
            } catch (e: Exception) {
                _submissionState.value = Resource.Error(
                    e.localizedMessage ?: context.getString(R.string.submission_failed)
                )
            }
        }
    }

    /**
     * Reset submission state
     */
    fun resetSubmissionState() {
        _submissionState.value = null
    }

    /**
     * Reset form
     */
    fun resetForm() {
        _patientsVisited.value = ""
        _vaccinationsGiven.value = ""
        _healthEducation.value = ""
        _challenges.value = ""
        _notes.value = ""
    }
}
