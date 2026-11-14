package com.afyaquest.app.data.remote

import com.afyaquest.app.util.TokenManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * What happens when the API answers 401: the session is refreshed once and the request repeated,
 * and a session the server has ended sends the app back to Login instead of failing for ever.
 * A local web server plays both the API and Cognito.
 */
class TokenAuthenticatorTest {

    private lateinit var api: MockWebServer
    private lateinit var cognito: MockWebServer
    private lateinit var tokenManager: TokenManager
    private lateinit var client: OkHttpClient

    /** The id token the fake session currently holds. */
    @Volatile private var storedIdToken = "expired-id-token"
    private val refreshes = AtomicInteger(0)

    @Before
    fun setup() {
        api = MockWebServer()
        cognito = MockWebServer()
        // The API accepts only the refreshed token.
        api.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.getHeader("Authorization") == "Bearer fresh-id-token") MockResponse().setBody("ok")
                else MockResponse().setResponseCode(401).setBody("{\"message\":\"Unauthorized\"}")
        }
        api.start()
        cognito.start()

        tokenManager = mock()
        whenever(tokenManager.getIdToken()).thenAnswer { storedIdToken }
        whenever(tokenManager.getRefreshToken()).thenReturn("refresh-token")
        doAnswer { storedIdToken = it.getArgument(1); null }
            .whenever(tokenManager).updateTokens(any(), any(), anyOrNull())

        val authenticator = TokenAuthenticator(tokenManager).apply { refreshUrl = cognito.url("/").toString() }
        client = OkHttpClient.Builder().authenticator(authenticator).build()
    }

    @After
    fun tearDown() {
        api.shutdown()
        cognito.shutdown()
    }

    private fun cognitoRefreshes() {
        cognito.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                refreshes.incrementAndGet()
                Thread.sleep(150) // long enough for other 401s to arrive meanwhile
                return MockResponse().setBody(
                    "{\"AuthenticationResult\":{\"AccessToken\":\"fresh-access-token\",\"IdToken\":\"fresh-id-token\"}}"
                )
            }
        }
    }

    private fun call(token: String = "expired-id-token") = client.newCall(
        Request.Builder().url(api.url("/user/assignments")).header("Authorization", "Bearer $token").build()
    ).execute()

    @Test
    fun `an expired token is refreshed once and the request is repeated with the new id token`() {
        cognitoRefreshes()

        call().use { response ->
            assertEquals(200, response.code)
            assertEquals("ok", response.body?.string())
        }

        verify(tokenManager).updateTokens("fresh-access-token", "fresh-id-token", null)
        assertEquals(1, refreshes.get())
        verify(tokenManager, never()).expireSession()
    }

    @Test
    fun `requests that fail together share a single refresh`() {
        cognitoRefreshes()
        val pool = Executors.newFixedThreadPool(5)
        val done = CountDownLatch(5)
        val codes = java.util.Collections.synchronizedList(mutableListOf<Int>())

        // Home fires several requests at once; with an expired token they all get 401 together.
        repeat(5) {
            pool.execute {
                call().use { codes.add(it.code) }
                done.countDown()
            }
        }

        assertTrue(done.await(10, TimeUnit.SECONDS))
        assertEquals(List(5) { 200 }, codes)
        assertEquals("one refresh for all of them", 1, refreshes.get())
        pool.shutdown()
    }

    @Test
    fun `a refresh token the server no longer accepts ends the session`() {
        cognito.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("{\"__type\":\"NotAuthorizedException\",\"message\":\"Refresh Token has expired\"}")
        )

        call().use { assertEquals(401, it.code) }

        verify(tokenManager).expireSession()
        verify(tokenManager, never()).updateTokens(any(), any(), anyOrNull())
    }

    @Test
    fun `a temporary failure of the refresh does not sign anybody out`() {
        cognito.enqueue(MockResponse().setResponseCode(500).setBody("{\"__type\":\"InternalErrorException\"}"))
        call().use { assertEquals(401, it.code) }

        cognito.enqueue(MockResponse().setResponseCode(400).setBody("{\"__type\":\"TooManyRequestsException\"}"))
        call().use { assertEquals(401, it.code) }

        verify(tokenManager, never()).expireSession()
    }

    @Test
    fun `a request without a session token is left alone`() {
        // Signing in with a wrong password is a 401 too; that is not an expired session.
        client.newCall(Request.Builder().url(api.url("/auth/login")).build()).execute().use {
            assertEquals(401, it.code)
        }

        assertEquals(0, cognito.requestCount)
        verify(tokenManager, never()).expireSession()
    }

    @Test
    fun `a refreshed token that is still refused is not refreshed again`() {
        // Cognito hands out a token the API does not accept: one retry, then give up (no loop).
        cognito.enqueue(
            MockResponse().setBody("{\"AuthenticationResult\":{\"AccessToken\":\"a\",\"IdToken\":\"still-bad\"}}")
        )

        call().use { assertEquals(401, it.code) }

        assertEquals(1, cognito.requestCount)
        assertEquals(2, api.requestCount)
    }

    @Test
    fun `a rotated refresh token is stored`() {
        cognito.enqueue(
            MockResponse().setBody(
                "{\"AuthenticationResult\":{\"AccessToken\":\"fresh-access-token\",\"IdToken\":\"fresh-id-token\",\"RefreshToken\":\"next-refresh\"}}"
            )
        )

        call().use { assertEquals(200, it.code) }

        verify(tokenManager, times(1)).updateTokens("fresh-access-token", "fresh-id-token", "next-refresh")
    }
}
