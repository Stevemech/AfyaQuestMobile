package com.example.afyaquest.data.repository

import com.example.afyaquest.data.local.dao.ReportDao
import com.example.afyaquest.data.local.entity.PendingReportEntity
import com.example.afyaquest.data.local.entity.ReportEntity
import com.example.afyaquest.domain.model.DailyReport
import com.example.afyaquest.sync.SyncManager
import com.example.afyaquest.util.Resource
import com.example.afyaquest.util.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for daily report operations.
 * Saves reports locally and queues them for sync.
 */
@Singleton
class ReportsRepository @Inject constructor(
    private val reportDao: ReportDao,
    private val syncManager: SyncManager,
    private val tokenManager: TokenManager
) {

    /**
     * Save a report to Room and queue it for sync.
     */
    fun saveReport(report: DailyReport): Flow<Resource<String>> = flow {
        emit(Resource.Loading())

        try {
            val userId = tokenManager.getUserId()
            if (userId.isNullOrBlank()) {
                emit(Resource.Error("Not authenticated"))
                return@flow
            }

            // Insert into reports table
            val reportEntity = ReportEntity(
                id = report.id,
                userId = userId,
                date = report.date,
                patientsVisited = report.patientsVisited,
                vaccinationsGiven = report.vaccinationsGiven,
                healthEducation = report.healthEducation,
                challenges = report.challenges,
                notes = report.notes,
                isSynced = false,
                createdAt = Date(),
                updatedAt = Date()
            )
            reportDao.insertReport(reportEntity)

            // Queue for sync
            val pendingReport = PendingReportEntity(
                userId = userId,
                date = report.date,
                patientsVisited = report.patientsVisited,
                vaccinationsGiven = report.vaccinationsGiven,
                healthEducation = report.healthEducation,
                challenges = report.challenges,
                notes = report.notes
            )
            syncManager.queueReport(pendingReport)

            emit(Resource.Success("Report saved successfully"))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to save report"))
        }
    }

    /**
     * Get all reports for the current user as a Flow.
     */
    fun getReportsForCurrentUser(): Flow<Resource<List<DailyReport>>> {
        val userId = tokenManager.getUserId()
        if (userId.isNullOrBlank()) {
            return flow { emit(Resource.Error("Not authenticated")) }
        }

        return reportDao.getReportsByUserFlow(userId).map { entities ->
            Resource.Success(entities.map { it.toDailyReport() })
        }
    }

    /**
     * Check if a report exists for the given date.
     */
    suspend fun hasReportForToday(date: String): Boolean {
        val userId = tokenManager.getUserId() ?: return false
        return reportDao.getReportByUserAndDate(userId, date) != null
    }

    /**
     * Delete a report by ID.
     */
    suspend fun deleteReport(reportId: String) {
        reportDao.deleteReportById(reportId)
    }

    private fun ReportEntity.toDailyReport() = DailyReport(
        id = id,
        date = date,
        patientsVisited = patientsVisited,
        vaccinationsGiven = vaccinationsGiven,
        healthEducation = healthEducation,
        challenges = challenges ?: "",
        notes = notes ?: "",
        isSynced = isSynced
    )
}
