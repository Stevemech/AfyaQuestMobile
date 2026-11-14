package com.afyaquest.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiErrorsTest {

    @Test
    fun `the error field of a backend error body is what the user sees`() {
        val body = """{"error":"User email not confirmed","details":"User is not confirmed."}"""
        assertEquals("User email not confirmed", apiErrorMessage(body, "Login failed"))
    }

    @Test
    fun `message is used when there is no error field`() {
        assertEquals("Unauthorized", apiErrorMessage("""{"message":"Unauthorized"}""", "Login failed"))
        assertEquals("Unauthorized", apiErrorMessage("""{"error":"  ","message":"Unauthorized"}""", "Login failed"))
    }

    @Test
    fun `anything that is not a usable error object falls back`() {
        assertEquals("Login failed", apiErrorMessage(null, "Login failed"))
        assertEquals("Login failed", apiErrorMessage("", "Login failed"))
        assertEquals("Login failed", apiErrorMessage("   ", "Login failed"))
        assertEquals("Login failed", apiErrorMessage("<html><body>502 Bad Gateway</body></html>", "Login failed"))
        assertEquals("Login failed", apiErrorMessage("Service Unavailable", "Login failed"))
        assertEquals("Login failed", apiErrorMessage("""["not","an","object"]""", "Login failed"))
        assertEquals("Login failed", apiErrorMessage("""{"details":"only details"}""", "Login failed"))
        assertEquals("Login failed", apiErrorMessage("""{"error":{"nested":true}}""", "Login failed"))
        assertEquals("Login failed", apiErrorMessage("""{"error":"unterminated""", "Login failed"))
    }
}
