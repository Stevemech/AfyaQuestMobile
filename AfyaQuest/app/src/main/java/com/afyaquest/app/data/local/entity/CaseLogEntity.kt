package com.afyaquest.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.afyaquest.app.data.local.converters.DateConverter
import java.util.Date

/**
 * A completed (or abandoned-at-disposition) emergency triage assessment, stored
 * offline-first. Mirrors [ReportEntity]'s isSynced pattern so it can ride the
 * existing sync machinery (backend sync wired in M4).
 *
 * [id] is a per-assessment UUID generated offline — it doubles as an idempotency
 * key so re-logging the same session (e.g. after stepping back and re-answering)
 * REPLACEs rather than duplicates.
 */
@Entity(tableName = "case_logs")
@TypeConverters(DateConverter::class)
data class CaseLogEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val treeVersion: String,
    val language: String,
    val mechanism: String?,            // trauma | medical | unknown | null
    val dispositionId: String,
    val dispositionLevel: String,      // early_exit | immediate | priority | stable
    val flagsJson: String,             // serialized accumulated flag map
    val pathJson: String,              // serialized ordered (nodeId, optionLetter) trail
    val completedAt: Date,
    val isSynced: Boolean = false,
    val createdAt: Date = Date()
)
