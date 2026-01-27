package com.example.afyaquest.data.local.dao

import androidx.room.*
import com.example.afyaquest.data.local.entity.HealthFacilityEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for HealthFacility operations.
 */
@Dao
interface HealthFacilityDao {
    @Query("SELECT * FROM health_facilities WHERE id = :facilityId")
    suspend fun getFacilityById(facilityId: String): HealthFacilityEntity?

    @Query("SELECT * FROM health_facilities WHERE region = :region")
    suspend fun getFacilitiesByRegion(region: String): List<HealthFacilityEntity>

    @Query("SELECT * FROM health_facilities WHERE region = :region")
    fun getFacilitiesByRegionFlow(region: String): Flow<List<HealthFacilityEntity>>

    @Query("SELECT * FROM health_facilities WHERE type = :type")
    suspend fun getFacilitiesByType(type: String): List<HealthFacilityEntity>

    @Query("SELECT * FROM health_facilities WHERE isOperational = 1")
    suspend fun getOperationalFacilities(): List<HealthFacilityEntity>

    @Query("SELECT * FROM health_facilities WHERE isOperational = 1")
    fun getOperationalFacilitiesFlow(): Flow<List<HealthFacilityEntity>>

    @Query("SELECT * FROM health_facilities")
    suspend fun getAllFacilities(): List<HealthFacilityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacility(facility: HealthFacilityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacilities(facilities: List<HealthFacilityEntity>)

    @Update
    suspend fun updateFacility(facility: HealthFacilityEntity)

    @Delete
    suspend fun deleteFacility(facility: HealthFacilityEntity)

    @Query("DELETE FROM health_facilities")
    suspend fun deleteAllFacilities()
}
