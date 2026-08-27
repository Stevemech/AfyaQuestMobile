package com.afyaquest.app.presentation.report

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afyaquest.app.data.repository.ReportsRepository
import com.afyaquest.app.domain.model.DailyReport
import com.afyaquest.app.util.DateUtils
import com.afyaquest.app.util.LanguageManager
import com.afyaquest.app.util.Resource
import com.afyaquest.app.util.XpManager
import com.afyaquest.app.util.XpRewards
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.afyaquest.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/** Details of a report that was just saved, used to show the completion dialog. */
data class SubmittedReportInfo(
    val date: String,
    val xpAwarded: Int
)

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

    companion object {
        /** Patients visited, vaccinations given, health education topic. */
        const val REQUIRED_FIELD_COUNT = 3
    }

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

    /** Set after a successful save; the screen shows the completion dialog until cleared. */
    private val _lastSubmission = MutableStateFlow<SubmittedReportInfo?>(null)
    val lastSubmission: StateFlow<SubmittedReportInfo?> = _lastSubmission.asStateFlow()

    /** True after the user tapped Submit with empty required fields; drives field error states. */
    private val _validationAttempted = MutableStateFlow(false)
    val validationAttempted: StateFlow<Boolean> = _validationAttempted.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _reportHistory = MutableStateFlow<List<DailyReport>>(emptyList())
    val reportHistory: StateFlow<List<DailyReport>> = _reportHistory.asStateFlow()

    private val _historyLoading = MutableStateFlow(false)
    val historyLoading: StateFlow<Boolean> = _historyLoading.asStateFlow()

    /** The report already saved for today, if any (already-submitted guard). */
    val todayReport: StateFlow<DailyReport?> = _reportHistory
        .map { reports -> reports.firstOrNull { it.date == DateUtils.todayIso() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** True when any field has content (draft protection). */
    val hasDraft: StateFlow<Boolean> = combine(
        _patientsVisited, _vaccinationsGiven, _healthEducation, _challenges, _notes
    ) { patients, vaccinations, education, challenges, notes ->
        patients.isNotEmpty() || vaccinations.isNotEmpty() || education.isNotEmpty() ||
            challenges.isNotEmpty() || notes.isNotEmpty()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /** How many of the [REQUIRED_FIELD_COUNT] required fields are filled. */
    val requiredFieldsDone: StateFlow<Int> = combine(
        _patientsVisited, _vaccinationsGiven, _healthEducation
    ) { patients, vaccinations, education ->
        listOf(patients, vaccinations, education).count { it.isNotEmpty() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

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
     * Submit daily report. XP is awarded only for the first report of the day.
     */
    fun submitReport() {
        if (!isFormValid()) {
            _validationAttempted.value = true
            _submissionState.value = Resource.Error(context.getString(R.string.fill_required_fields))
            return
        }

        viewModelScope.launch {
            _submissionState.value = Resource.Loading()

            try {
                val today = DateUtils.todayIso()
                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                timestamp.timeZone = TimeZone.getTimeZone("UTC")

                // Decide on XP before saving so the new row cannot count as "already reported"
                val isFirstToday = !reportsRepository.hasReportForToday(today)

                val report = DailyReport(
                    id = System.currentTimeMillis().toString(),
                    date = today,
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
                            var xpAwarded = 0
                            if (isFirstToday) {
                                // Award XP for the first daily report of the day
                                xpManager.addXP(
                                    XpRewards.DAILY_REPORT,
                                    "Submitted daily report"
                                )
                                xpAwarded = XpRewards.DAILY_REPORT
                            }
                            resetForm()
                            _submissionState.value = null
                            _lastSubmission.value = SubmittedReportInfo(
                                date = today,
                                xpAwarded = xpAwarded
                            )
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
     * Delete a report by ID.
     */
    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            reportsRepository.deleteReport(reportId)
        }
    }

    /**
     * Reset submission state
     */
    fun resetSubmissionState() {
        _submissionState.value = null
    }

    /** Dismiss the completion dialog. */
    fun clearLastSubmission() {
        _lastSubmission.value = null
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
        _validationAttempted.value = false
    }
}
