package com.example.afyaquest.presentation.report

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afyaquest.domain.model.DailyReport
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
    private val xpManager: XpManager
    // TODO: Inject ReportsRepository when backend is ready
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

    val healthEducationTopics: List<String> get() = listOf(
        context.getString(R.string.category_hygiene),
        context.getString(R.string.category_nutrition),
        context.getString(R.string.category_disease_prevention),
        context.getString(R.string.category_maternal_health),
        context.getString(R.string.category_child_care)
    )

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

                // TODO: Submit to backend when repository is available
                // For now, just award XP and mark as successful

                // Award XP for submitting daily report
                xpManager.addXP(
                    XpRewards.DAILY_REPORT,
                    "Submitted daily report"
                )

                _submissionState.value = Resource.Success(context.getString(R.string.report_submitted_success))

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
        _submissionState.value = null
    }
}
