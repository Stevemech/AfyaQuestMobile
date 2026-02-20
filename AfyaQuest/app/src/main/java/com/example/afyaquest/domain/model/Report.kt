package com.example.afyaquest.domain.model

/**
 * Daily report model
 */
data class DailyReport(
    val id: String = "",
    val date: String,
    val timestamp: String = "",
    val patientsVisited: Int,
    val vaccinationsGiven: Int,
    val healthEducation: String,
    val challenges: String = "",
    val notes: String = "",
    val isSynced: Boolean = false
)

/**
 * Report submission response
 */
data class ReportSubmissionResponse(
    val message: String,
    val reportId: String
)
