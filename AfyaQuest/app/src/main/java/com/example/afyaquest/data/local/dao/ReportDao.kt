package com.example.afyaquest.data.local.dao

import androidx.room.*
import com.example.afyaquest.data.local.entity.ReportEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Report operations.
 */
@Dao
interface ReportDao {
    @Query("SELECT * FROM reports WHERE id = :reportId")
    suspend fun getReportById(reportId: String): ReportEntity?

    @Query("SELECT * FROM reports WHERE userId = :userId ORDER BY date DESC")
    suspend fun getReportsByUser(userId: String): List<ReportEntity>

    @Query("SELECT * FROM reports WHERE userId = :userId ORDER BY date DESC")
    fun getReportsByUserFlow(userId: String): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE userId = :userId AND date = :date")
    suspend fun getReportByUserAndDate(userId: String, date: String): ReportEntity?

    @Query("SELECT * FROM reports WHERE isSynced = 0")
    suspend fun getUnsyncedReports(): List<ReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Update
    suspend fun updateReport(report: ReportEntity)

    @Query("UPDATE reports SET isSynced = 1 WHERE id = :reportId")
    suspend fun markReportAsSynced(reportId: String)

    @Delete
    suspend fun deleteReport(report: ReportEntity)

    @Query("DELETE FROM reports WHERE id = :reportId")
    suspend fun deleteReportById(reportId: String)

    @Query("DELETE FROM reports WHERE userId = :userId")
    suspend fun deleteReportsByUser(userId: String)
}
