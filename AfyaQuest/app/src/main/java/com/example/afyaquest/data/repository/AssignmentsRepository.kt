package com.example.afyaquest.data.repository

import com.example.afyaquest.data.remote.ApiService
import com.example.afyaquest.data.remote.dto.AssignmentsResponse
import com.example.afyaquest.util.Resource
import com.example.afyaquest.util.TokenManager
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
            val token = tokenManager.getAccessToken()
            if (token == null) {
                emit(Resource.Error("Not authenticated"))
                return@flow
            }

            val response = apiService.getAssignments("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error(response.message() ?: "Failed to get assignments"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        }
    }
}
