package com.afyaquest.app.data.repository

import com.afyaquest.app.data.local.dao.ReportDao
import com.afyaquest.app.data.local.entity.PendingReportEntity
import com.afyaquest.app.data.local.entity.ReportEntity
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.domain.model.DailyReport
import com.afyaquest.app.sync.SyncManager
import com.afyaquest.app.util.DateUtils
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.Resource
import com.afyaquest.app.util.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    private val tokenManager: TokenManager,
    private val progressDataStore: ProgressDataStore,
    private val apiService: ApiService
) {

    /** The account whose reports the server has already been asked for in this run of the app. */
    @Volatile
    private var restoreAnsweredFor: String? = null

    /**
     * Bring the reports this account already uploaded onto this phone. It only runs when the phone
     * has no report for the account at all: a new phone, or a reinstall. Without it Home asked for
     * a daily report that was already submitted, and the report history was empty.
     *
     * A phone that already has reports is left alone, so a report the CHV deleted from their
     * history does not come back.
     *
     * @return how many reports were added
     */
    suspend fun restoreFromServerIfEmpty(): Int {
        val userId = tokenManager.getUserId()?.takeIf { it.isNotBlank() } ?: return 0
        val token = tokenManager.getIdToken()?.takeIf { it.isNotBlank() } ?: return 0
        // A CHV who has never sent a report has none to restore; asking once is enough.
        if (restoreAnsweredFor == userId) return 0
        if (reportDao.getReportsByUser(userId).isNotEmpty()) return 0

        val uploaded = try {
            apiService.getAllReports("Bearer $token").takeIf { it.isSuccessful }?.body()
        } catch (e: Exception) {
            null
        } ?: return 0

        restoreAnsweredFor = userId
        var added = 0
        for (item in uploaded) {
            val date = (item["date"] as? String)?.takeIf { it.isNotBlank() } ?: continue
            reportDao.insertReport(
                ReportEntity(
                    id = (item["id"] as? String)?.takeIf { it.isNotBlank() } ?: "$userId-$date",
                    userId = userId,
                    date = date,
                    patientsVisited = (item["patientsVisited"] as? Number)?.toInt() ?: 0,
                    vaccinationsGiven = (item["vaccinationsGiven"] as? Number)?.toInt() ?: 0,
                    healthEducation = item["healthEducation"] as? String ?: "",
                    challenges = item["challenges"] as? String,
                    notes = item["notes"] as? String,
                    isSynced = true
                )
            )
            progressDataStore.markReportDate(date)
            added++
        }
        return added
    }

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
            // Part of the account's progress (achievements, and restored on another phone)
            progressDataStore.markReportDate(report.date)

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
     * Emits true whenever a report dated today exists locally for the current user.
     * Used by the Home hub to tick the "Daily Report" task. Emits false when nobody is logged in.
     */
    fun observeReportSubmittedToday(): Flow<Boolean> {
        val userId = tokenManager.getUserId()
        if (userId.isNullOrBlank()) return flowOf(false)
        return reportDao.getReportsByUserFlow(userId).map { list ->
            val today = DateUtils.todayIso()
            list.any { it.date == today }
        }
    }

    /**
     * Whether a report was already submitted on [date]. The answer comes from the account's
     * record of report days as well as from the saved reports: deleting today's report from the
     * history and submitting again must not pay the day's XP a second time.
     */
    suspend fun hasReportForToday(date: String): Boolean {
        val userId = tokenManager.getUserId() ?: return false
        return progressDataStore.getReportDates().first().contains(date) ||
            reportDao.getReportByUserAndDate(userId, date) != null
    }

    /**
     * Delete a report by ID. A report that has not been uploaded yet is also taken out of the
     * upload queue; before, the supervisor still received a report the CHV had deleted.
     */
    suspend fun deleteReport(reportId: String) {
        val report = reportDao.getReportById(reportId)
        reportDao.deleteReportById(reportId)
        if (report != null && !report.isSynced) {
            syncManager.dropQueuedReport(report.userId, report.date)
        }
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
