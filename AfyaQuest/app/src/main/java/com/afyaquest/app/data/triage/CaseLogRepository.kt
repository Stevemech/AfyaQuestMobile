package com.afyaquest.app.data.triage

import com.afyaquest.app.data.local.dao.CaseLogDao
import com.afyaquest.app.data.local.entity.CaseLogEntity
import com.afyaquest.app.domain.triage.TriageState
import com.afyaquest.app.util.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists completed emergency assessments offline and exposes the current user's
 * case history. Backend sync (POST /cases) is wired in M4 via [CaseLogDao.getUnsynced].
 */
@Singleton
class CaseLogRepository @Inject constructor(
    private val caseLogDao: CaseLogDao,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

    /**
     * Record (or update, via REPLACE on the same [caseId]) the assessment that
     * has reached a disposition. [caseId] is stable per assessment session, so
     * stepping back and re-answering updates one record rather than duplicating.
     */
    suspend fun logDisposition(
        caseId: String,
        state: TriageState,
        dispositionLevel: String,
        treeVersion: String,
        language: String
    ) {
        caseLogDao.insert(
            CaseLogEntity(
                id = caseId,
                userId = tokenManager.getUserId() ?: UNKNOWN_USER,
                treeVersion = treeVersion,
                language = language,
                mechanism = state.flags["mechanism"],
                dispositionId = state.currentId,
                dispositionLevel = dispositionLevel,
                flagsJson = gson.toJson(state.flags),
                pathJson = gson.toJson(state.path),
                completedAt = Date()
            )
        )
    }

    fun observeCurrentUserCases(): Flow<List<CaseLogEntity>> {
        val userId = tokenManager.getUserId() ?: return flowOf(emptyList())
        return caseLogDao.observeByUser(userId)
    }

    private companion object {
        const val UNKNOWN_USER = "unknown"
    }
}
