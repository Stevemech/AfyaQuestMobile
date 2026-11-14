package com.afyaquest.app.util

import com.google.gson.JsonParser

/**
 * Turns an API error body into text that can be shown to the user.
 *
 * The backend answers failures with JSON such as
 * `{"error":"Invalid email or password","details":"Incorrect username or password."}`.
 * Only the `error` (or `message`) field is meant for people; showing the whole body puts raw JSON
 * on screen. Anything that is not such an object (an HTML gateway page, an empty body) falls back
 * to [fallback].
 */
fun apiErrorMessage(errorBody: String?, fallback: String): String {
    if (errorBody.isNullOrBlank()) return fallback
    return try {
        val json = JsonParser.parseString(errorBody)
        if (!json.isJsonObject) return fallback
        val body = json.asJsonObject
        sequenceOf("error", "message")
            .mapNotNull { key -> body.get(key) }
            .filter { it.isJsonPrimitive }
            .map { it.asString.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: fallback
    } catch (e: Exception) {
        fallback
    }
}
