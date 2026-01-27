package com.example.afyaquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.afyaquest.data.local.converters.DateConverter
import java.util.Date

/**
 * Room entity representing a daily report.
 * Used for offline-first reporting with sync capability.
 */
@Entity(tableName = "reports")
@TypeConverters(DateConverter::class)
data class ReportEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val date: String, // Format: YYYY-MM-DD
    val patientsVisited: Int = 0,
    val vaccinationsGiven: Int = 0,
    val healthEducationSessions: Int = 0,
    val challenges: String? = null,
    val notes: String? = null,
    val isSynced: Boolean = false, // Track sync status for offline support
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
