package com.afyaquest.app.data.repository

import android.util.Log
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.domain.model.ItineraryStop
import com.afyaquest.app.util.DateUtils
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.TokenManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Today's route (the stops a supervisor planned for this CHV).
 *
 * The route is kept on the phone each time it is downloaded. Home downloads it when the app opens,
 * so a CHV who signed in where there was a connection still has the stops, and can mark them
 * visited, with no connection in the field, even if they never opened the map beforehand.
 */
@Singleton
class ItineraryRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val progressDataStore: ProgressDataStore
) {

    private val gson = Gson()

    /**
     * Today's stops: from the server when it can be reached, otherwise the copy downloaded earlier
     * today. Null when there is neither (the caller shows an error with Retry).
     */
    suspend fun todayStops(): List<ItineraryStop>? = refreshToday() ?: cachedToday()

    /**
     * Download today's route and keep it on the phone. Null when the server could not be reached.
     */
    suspend fun refreshToday(): List<ItineraryStop>? {
        val fresh = fetchToday() ?: return null
        progressDataStore.setItineraryTotal(fresh.size)
        progressDataStore.cacheItinerary(gson.toJson(fresh))
        // Visits the server already knows about count on this phone too. Without this, signing in
        // on a new phone (or again after another account used this one) showed "0 of 4 stops
        // visited" on Home although two were done.
        val visited = fresh.filter { it.completed }.map { it.id }
        if (visited.isNotEmpty()) progressDataStore.markStopsCompleted(visited)
        return fresh
    }

    /** The route downloaded earlier today, or null. */
    suspend fun cachedToday(): List<ItineraryStop>? = try {
        progressDataStore.getCachedItinerary()?.let { json ->
            gson.fromJson<List<ItineraryStop>>(json, object : TypeToken<List<ItineraryStop>>() {}.type)
        }
    } catch (e: Exception) {
        null
    }

    private suspend fun fetchToday(): List<ItineraryStop>? = try {
        val idToken = tokenManager.getIdToken()
        val response = if (idToken != null) apiService.getItineraries("Bearer $idToken") else null
        val body = response?.takeIf { it.isSuccessful }?.body()
        if (body == null) {
            null
        } else {
            // The API returns every itinerary the admin has created, newest date first. Only
            // today's is "your stops for the day": showing tomorrow's (or last week's) as today
            // would let stops be marked visited on the wrong day.
            val today = DateUtils.todayIso()
            body.itineraries.firstOrNull { it.date == today }?.stops.orEmpty().map { stopDto ->
                ItineraryStop(
                    order = stopDto.order,
                    id = stopDto.houseId?.takeIf { it.isNotBlank() } ?: "stop-${stopDto.order}",
                    label = stopDto.label,
                    address = stopDto.address,
                    latitude = stopDto.latitude,
                    longitude = stopDto.longitude,
                    description = stopDto.description,
                    completed = stopDto.completed
                )
            }
        }
    } catch (e: Exception) {
        Log.d("ItineraryRepository", "Could not load the itinerary: ${e.message}")
        null
    }
}
