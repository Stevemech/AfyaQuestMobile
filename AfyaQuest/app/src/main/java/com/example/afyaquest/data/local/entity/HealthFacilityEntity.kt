package com.example.afyaquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.afyaquest.data.local.converters.StringListConverter

/**
 * Room entity representing a health facility.
 */
@Entity(tableName = "health_facilities")
@TypeConverters(StringListConverter::class)
data class HealthFacilityEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String, // 'hospital', 'clinic', 'health_center', 'dispensary'
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val phoneNumber: String? = null,
    val services: List<String> = emptyList(), // List of services offered
    val operatingHours: String? = null,
    val isOperational: Boolean = true
)
