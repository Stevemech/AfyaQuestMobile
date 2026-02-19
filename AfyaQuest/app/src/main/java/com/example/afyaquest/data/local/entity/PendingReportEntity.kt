package com.example.afyaquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Pending daily report waiting to be synced
 */
@Entity(tableName = "pending_reports")
data class PendingReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: String,
    val patientsVisited: Int,
    val vaccinationsGiven: Int,
    val healthEducation: String,
    val challenges: String,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
