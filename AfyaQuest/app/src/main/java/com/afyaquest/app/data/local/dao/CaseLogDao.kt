package com.afyaquest.app.data.local.dao

import androidx.room.*
import com.afyaquest.app.data.local.entity.CaseLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for emergency triage case logs.
 */
@Dao
interface CaseLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(caseLog: CaseLogEntity)

    @Query("SELECT * FROM case_logs WHERE userId = :userId ORDER BY completedAt DESC")
    fun observeByUser(userId: String): Flow<List<CaseLogEntity>>

    @Query("SELECT * FROM case_logs WHERE userId = :userId ORDER BY completedAt DESC")
    suspend fun getByUser(userId: String): List<CaseLogEntity>

    @Query("SELECT * FROM case_logs WHERE id = :id")
    suspend fun getById(id: String): CaseLogEntity?

    @Query("SELECT * FROM case_logs WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsynced(): List<CaseLogEntity>

    @Query("SELECT COUNT(*) FROM case_logs WHERE isSynced = 0")
    fun unsyncedCount(): Flow<Int>

    @Query("UPDATE case_logs SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}
