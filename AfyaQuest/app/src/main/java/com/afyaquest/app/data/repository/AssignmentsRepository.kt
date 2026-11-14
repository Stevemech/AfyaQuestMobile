package com.afyaquest.app.data.repository

import android.util.Log
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.data.remote.dto.AssignmentDto
import com.afyaquest.app.data.remote.dto.AssignmentsResponse
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.Resource
import com.afyaquest.app.util.TokenManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssignmentsRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val progressDataStore: ProgressDataStore
) {

    private val gson = Gson()

    /**
     * The tasks the supervisor assigned to this CHV.
     *
     * The list is kept on the phone each time it is downloaded, and the saved copy is shown
     * first: the screen has content at once, with or without a connection, and the server's
     * answer replaces it when it arrives. (It used to be the other way round, so on a weak
     * connection the list sat behind a spinner until the request gave up.) "Loading" is only
     * reported when nothing is saved yet, and an error only when there is no saved copy either.
     */
    fun getAssignments(): Flow<Resource<AssignmentsResponse>> = flow {
        val cached = cachedAssignments()
        if (cached != null) emit(Resource.Success(AssignmentsResponse(cached))) else emit(Resource.Loading())

        val fresh = fetchAssignments()
        when {
            fresh != null -> {
                if (fresh != cached) {
                    progressDataStore.cacheAssignments(gson.toJson(fresh))
                    emit(Resource.Success(AssignmentsResponse(fresh)))
                }
            }
            cached == null -> emit(Resource.Error("Failed to get assignments"))
        }
    }

    /** The assignments from the server, or null when they could not be loaded. */
    private suspend fun fetchAssignments(): List<AssignmentDto>? = try {
        val token = tokenManager.getIdToken()
        val response = if (token != null) apiService.getAssignments("Bearer $token") else null
        val body = response?.takeIf { it.isSuccessful }?.body()
        if (body == null) {
            Log.e("AssignmentsRepo", "Could not load assignments (code ${response?.code()})")
            null
        } else {
            // Drop ghost rows (old UpdateItem upserts) that lack assignedAt
            body.assignments.filter { !it.assignedAt.isNullOrBlank() }
        }
    } catch (e: Exception) {
        Log.e("AssignmentsRepo", "Network error: ${e.message}")
        null
    }

    private suspend fun cachedAssignments(): List<AssignmentDto>? = try {
        progressDataStore.getCachedAssignments()?.let { json ->
            gson.fromJson<List<AssignmentDto>>(json, object : TypeToken<List<AssignmentDto>>() {}.type)
        }
    } catch (e: Exception) {
        null
    }
}
