package com.example.afyaquest.data.local.dao

import androidx.room.*
import com.example.afyaquest.data.local.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Progress operations.
 */
@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE userId = :userId AND contentId = :contentId AND contentType = :contentType")
    suspend fun getProgress(userId: String, contentId: String, contentType: String): ProgressEntity?

    @Query("SELECT * FROM progress WHERE userId = :userId AND contentType = :contentType")
    suspend fun getProgressByType(userId: String, contentType: String): List<ProgressEntity>

    @Query("SELECT * FROM progress WHERE userId = :userId AND contentType = :contentType")
    fun getProgressByTypeFlow(userId: String, contentType: String): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE userId = :userId AND completed = 1")
    suspend fun getCompletedProgress(userId: String): List<ProgressEntity>

    @Query("SELECT * FROM progress WHERE userId = :userId AND completed = 1")
    fun getCompletedProgressFlow(userId: String): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE isSynced = 0")
    suspend fun getUnsyncedProgress(): List<ProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: ProgressEntity)

    @Update
    suspend fun updateProgress(progress: ProgressEntity)

    @Query("UPDATE progress SET isSynced = 1 WHERE userId = :userId AND contentId = :contentId AND contentType = :contentType")
    suspend fun markProgressAsSynced(userId: String, contentId: String, contentType: String)

    @Delete
    suspend fun deleteProgress(progress: ProgressEntity)

    @Query("DELETE FROM progress WHERE userId = :userId")
    suspend fun deleteProgressByUser(userId: String)
}
