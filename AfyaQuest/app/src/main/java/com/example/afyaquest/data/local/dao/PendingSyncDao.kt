package com.example.afyaquest.data.local.dao

import androidx.room.*
import com.example.afyaquest.data.local.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for pending sync items
 */
@Dao
interface PendingSyncDao {

    // Reports
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingReport(report: PendingReportEntity): Long

    @Query("SELECT * FROM pending_reports WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedReports(): List<PendingReportEntity>

    @Query("UPDATE pending_reports SET synced = 1 WHERE id = :id")
    suspend fun markReportSynced(id: Long)

    @Query("DELETE FROM pending_reports WHERE synced = 1 AND createdAt < :olderThan")
    suspend fun deleteSyncedReports(olderThan: Long)

    // Quizzes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingQuiz(quiz: PendingQuizEntity): Long

    @Query("SELECT * FROM pending_quizzes WHERE synced = 0 ORDER BY submittedAt ASC")
    suspend fun getUnsyncedQuizzes(): List<PendingQuizEntity>

    @Query("UPDATE pending_quizzes SET synced = 1 WHERE id = :id")
    suspend fun markQuizSynced(id: Long)

    @Query("DELETE FROM pending_quizzes WHERE synced = 1 AND submittedAt < :olderThan")
    suspend fun deleteSyncedQuizzes(olderThan: Long)

    // Chats
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingChat(chat: PendingChatEntity): Long

    @Query("SELECT * FROM pending_chats WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedChats(): List<PendingChatEntity>

    @Query("UPDATE pending_chats SET synced = 1, responseReceived = :received, response = :response WHERE id = :id")
    suspend fun markChatSynced(id: Long, received: Boolean, response: String?)

    @Query("DELETE FROM pending_chats WHERE synced = 1 AND createdAt < :olderThan")
    suspend fun deleteSyncedChats(olderThan: Long)

    // Client Visits
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingClientVisit(visit: PendingClientVisitEntity): Long

    @Query("SELECT * FROM pending_client_visits WHERE synced = 0 ORDER BY updatedAt ASC")
    suspend fun getUnsyncedClientVisits(): List<PendingClientVisitEntity>

    @Query("UPDATE pending_client_visits SET synced = 1 WHERE id = :id")
    suspend fun markClientVisitSynced(id: Long)

    @Query("DELETE FROM pending_client_visits WHERE synced = 1 AND updatedAt < :olderThan")
    suspend fun deleteSyncedClientVisits(olderThan: Long)

    // Count unsynced items
    @Query("SELECT COUNT(*) FROM pending_reports WHERE synced = 0")
    fun getUnsyncedReportsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_quizzes WHERE synced = 0")
    fun getUnsyncedQuizzesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_chats WHERE synced = 0")
    fun getUnsyncedChatsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_client_visits WHERE synced = 0")
    fun getUnsyncedClientVisitsCount(): Flow<Int>
}
