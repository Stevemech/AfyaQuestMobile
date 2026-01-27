package com.example.afyaquest.data.local.dao

import androidx.room.*
import com.example.afyaquest.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Achievement operations.
 */
@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY unlockedDate DESC")
    suspend fun getAchievementsByUser(userId: String): List<AchievementEntity>

    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY unlockedDate DESC")
    fun getAchievementsByUserFlow(userId: String): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE userId = :userId AND category = :category")
    suspend fun getAchievementsByCategory(userId: String, category: String): List<AchievementEntity>

    @Query("SELECT * FROM achievements WHERE isSynced = 0")
    suspend fun getUnsyncedAchievements(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Query("UPDATE achievements SET isSynced = 1 WHERE userId = :userId AND achievementId = :achievementId")
    suspend fun markAchievementAsSynced(userId: String, achievementId: String)

    @Delete
    suspend fun deleteAchievement(achievement: AchievementEntity)

    @Query("DELETE FROM achievements WHERE userId = :userId")
    suspend fun deleteAchievementsByUser(userId: String)
}
