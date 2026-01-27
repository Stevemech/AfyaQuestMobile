package com.example.afyaquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Pending client visit status update waiting to be synced
 */
@Entity(tableName = "pending_client_visits")
data class PendingClientVisitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val clientId: String,
    val status: String, // PENDING, VISITED, CANCELLED
    val visitDate: String,
    val notes: String?,
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
