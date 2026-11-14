package com.afyaquest.app.data.remote

import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

/** A name lookup must fail on time instead of hanging the request (and the Sign In button). */
class TimeoutDnsTest {

    private val address = InetAddress.getByAddress(byteArrayOf(10, 0, 0, 1))

    private fun dns(answer: (String) -> List<InetAddress>) = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> = answer(hostname)
    }

    @Test
    fun `a lookup that answers in time is passed through`() {
        val dns = TimeoutDns(timeoutSeconds = 2, delegate = dns { listOf(address) })

        assertEquals(listOf(address), dns.lookup("api.example.org"))
    }

    @Test
    fun `a lookup that hangs fails after the deadline`() {
        val hanging = dns { Thread.sleep(30_000); listOf(address) }
        val dns = TimeoutDns(timeoutSeconds = 1, delegate = hanging)

        val started = System.currentTimeMillis()
        try {
            dns.lookup("api.example.org")
            fail("expected the lookup to time out")
        } catch (e: UnknownHostException) {
            val waited = System.currentTimeMillis() - started
            assertTrue("gave up after $waited ms", waited in 900..3_000)
            assertTrue(e.message.orEmpty().contains("api.example.org"))
        }
    }

    @Test
    fun `an unknown host stays an unknown host`() {
        val dns = TimeoutDns(timeoutSeconds = 2, delegate = dns { throw UnknownHostException("no such host") })

        try {
            dns.lookup("nowhere.example.org")
            fail("expected UnknownHostException")
        } catch (e: UnknownHostException) {
            assertEquals("no such host", e.message)
        }
    }

    @Test
    fun `any other resolver failure is reported as an unknown host`() {
        val dns = TimeoutDns(timeoutSeconds = 2, delegate = dns { throw IllegalStateException("resolver broke") })

        try {
            dns.lookup("api.example.org")
            fail("expected UnknownHostException")
        } catch (e: UnknownHostException) {
            assertTrue(e.cause is IllegalStateException)
        }
    }

    @Test
    fun `one slow lookup does not block the next one`() {
        var calls = 0
        val dns = TimeoutDns(timeoutSeconds = 1, delegate = dns {
            if (calls++ == 0) Thread.sleep(30_000)
            listOf(address)
        })
        try { dns.lookup("slow.example.org") } catch (_: UnknownHostException) { }

        val started = System.currentTimeMillis()
        assertEquals(listOf(address), dns.lookup("fast.example.org"))
        assertFalse(System.currentTimeMillis() - started > 900)
    }

    @Test
    fun `cognito refusals that end the session are told apart from temporary ones`() {
        assertTrue(TokenAuthenticator.isSessionOver(400, """{"__type":"NotAuthorizedException","message":"Refresh Token has expired"}"""))
        assertTrue(TokenAuthenticator.isSessionOver(400, """{"__type":"UserNotFoundException","message":"User does not exist."}"""))
        // Throttling, an outage or an unreadable answer must not sign anybody out.
        assertFalse(TokenAuthenticator.isSessionOver(400, """{"__type":"TooManyRequestsException"}"""))
        assertFalse(TokenAuthenticator.isSessionOver(500, """{"__type":"NotAuthorizedException"}"""))
        assertFalse(TokenAuthenticator.isSessionOver(400, null))
    }
}
