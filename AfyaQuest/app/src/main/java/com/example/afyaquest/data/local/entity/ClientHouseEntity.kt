package com.example.afyaquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.afyaquest.data.local.converters.DateConverter
import java.util.Date

/**
 * Room entity representing a client house location for CHAs to visit.
 */
@Entity(tableName = "client_houses")
@TypeConverters(DateConverter::class)
data class ClientHouseEntity(
    @PrimaryKey
    val id: String,
    val userId: String, // The CHA responsible for this client
    val clientName: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val phoneNumber: String? = null,
    val status: String = "pending", // 'pending', 'visited', 'skipped'
    val notes: String? = null,
    val lastVisitDate: Date? = null,
    val isSynced: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
