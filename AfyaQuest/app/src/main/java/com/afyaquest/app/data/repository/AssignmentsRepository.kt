package com.afyaquest.app.data.repository

import android.util.Log
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.data.remote.dto.AssignmentsResponse
import com.afyaquest.app.util.Resource
import com.afyaquest.app.util.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssignmentsRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    fun getAssignments(): Flow<Resource<AssignmentsResponse>> = flow {
        emit(Resource.Loading())

        try {
            val token = tokenManager.getIdToken()
            if (token == null) {
                emit(Resource.Error("Not authenticated"))
                return@flow
            }

            val response = apiService.getAssignments("Bearer $token")
            Log.d("AssignmentsRepo", "API response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                // Drop ghost rows (old UpdateItem upserts) that lack assignedAt — even if API is not redeployed yet
                val valid = body.assignments.filter { !it.assignedAt.isNullOrBlank() }
                Log.d("AssignmentsRepo", "Got ${valid.size} valid assignments from API (${body.assignments.size} raw)")
                emit(Resource.Success(AssignmentsResponse(valid)))
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AssignmentsRepo", "API error: ${response.code()} - $errorBody")
                emit(Resource.Error(response.message() ?: "Failed to get assignments"))
            }
        } catch (e: Exception) {
            Log.e("AssignmentsRepo", "Network error: ${e.message}", e)
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        }
    }
}
