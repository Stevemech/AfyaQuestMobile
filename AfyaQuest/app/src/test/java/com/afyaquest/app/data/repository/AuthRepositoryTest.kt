package com.afyaquest.app.data.repository

import com.afyaquest.app.data.local.dao.UserDao
import com.afyaquest.app.data.local.entity.UserEntity
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.data.remote.dto.LoginResponse
import com.afyaquest.app.data.remote.dto.UserDto
import com.afyaquest.app.util.AuthErrors
import com.afyaquest.app.util.Resource
import com.afyaquest.app.util.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Sign-in outcomes: what is stored, what the user is told, and that a profile with gaps (old and
 * admin accounts have them) no longer breaks the sign-in.
 */
class AuthRepositoryTest {

    @Mock private lateinit var apiService: ApiService
    @Mock private lateinit var userDao: UserDao
    @Mock private lateinit var tokenManager: TokenManager

    private lateinit var repository: AuthRepository
    private val gson = Gson()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = AuthRepository(apiService, userDao, tokenManager)
    }

    /** The response exactly as Gson would build it from the server's JSON. */
    private fun serverSays(json: String) = runBlocking<Unit> {
        whenever(apiService.login(any())).thenReturn(Response.success(gson.fromJson(json, LoginResponse::class.java)))
    }

    private fun serverRefuses(code: Int, json: String) = runBlocking<Unit> {
        whenever(apiService.login(any()))
            .thenReturn(Response.error(code, json.toResponseBody("application/json".toMediaType())))
    }

    private fun signIn() = runBlocking { repository.login("chv@example.org", "secret!") }

    @Test
    fun `a successful sign-in stores the session and the profile`() = runBlocking<Unit> {
        serverSays(
            """{"accessToken":"a","idToken":"i","refreshToken":"r","expiresIn":3600,
               "user":{"id":"u1","email":"chv@example.org","name":"Amina","role":"cha","language":"en",
                       "level":2,"totalPoints":290,"rank":"Novice","currentStreak":1,"isActive":true,
                       "organization":"kibera-east","manualStatus":"active"}}"""
        )

        val result = signIn()

        assertTrue(result is Resource.Success)
        val user = result.data!!
        assertEquals("u1", user.id)
        assertEquals("Amina", user.name)
        assertEquals(290, user.totalPoints)
        assertTrue(user.clockedIn)
        verify(tokenManager).saveSession("a", "i", "r", "u1")
        val saved = argumentCaptor<UserEntity>()
        verify(userDao).insertUser(saved.capture())
        assertEquals("kibera-east", saved.firstValue.organization)
    }

    @Test
    fun `a profile with missing fields still signs in, with sensible defaults`() = runBlocking<Unit> {
        // An admin / old account: no language, rank, level, points, streak or isActive.
        serverSays(
            """{"accessToken":"a","idToken":"i","refreshToken":"r",
               "user":{"id":"u2","email":"admin@example.org","name":"Steve","role":"admin","organization":"kibera-east"}}"""
        )

        val result = signIn()

        assertTrue(result is Resource.Success)
        val user = result.data!!
        assertEquals("en", user.language)
        assertEquals("Beginner", user.rank)
        assertEquals(0, user.level)
        assertEquals(0, user.totalPoints)
        assertTrue("an account is enabled unless the server says otherwise", user.isActive)
        assertEquals(false, user.clockedIn)
        verify(tokenManager).saveSession("a", "i", "r", "u2")
    }

    @Test
    fun `an account without a profile is refused and nothing is stored`() = runBlocking<Unit> {
        // The password was right, but the profile behind it is only a stub (no id).
        serverSays("""{"accessToken":"a","idToken":"i","refreshToken":"r","user":{"lastActiveDate":"2026-11-12"}}""")

        val result = signIn()

        assertEquals(AuthErrors.ACCOUNT_INCOMPLETE, result.message)
        // A session saved here would open Home on the next launch with nobody really signed in.
        verify(tokenManager, never()).saveSession(any(), any(), any(), any())
        verify(userDao, never()).insertUser(any())
    }

    @Test
    fun `a reply without tokens is refused`() = runBlocking<Unit> {
        serverSays("""{"user":{"id":"u1","email":"chv@example.org","name":"Amina"}}""")

        assertEquals(AuthErrors.ACCOUNT_INCOMPLETE, signIn().message)
        verify(tokenManager, never()).saveSession(any(), any(), any(), any())
    }

    @Test
    fun `refusals from the server are explained`() {
        serverRefuses(401, """{"error":"Invalid email or password","details":"Incorrect username or password."}""")
        assertEquals(AuthErrors.INVALID_CREDENTIALS, signIn().message)

        serverRefuses(404, """{"error":"User not found"}""")
        assertEquals("an unknown email must look the same as a wrong password", AuthErrors.INVALID_CREDENTIALS, signIn().message)

        serverRefuses(403, """{"error":"User email not confirmed"}""")
        assertEquals(AuthErrors.EMAIL_NOT_CONFIRMED, signIn().message)

        serverRefuses(401, """{"error":"Your account is pending approval. An administrator will enable your account shortly."}""")
        assertTrue(signIn().message!!.startsWith("Your account is pending approval"))

        serverRefuses(500, """{"error":"Internal server error","details":"ResourceNotFoundException: table"}""")
        assertEquals("server internals are not shown to the user", AuthErrors.SERVER, signIn().message)

        serverRefuses(502, "<html>Bad Gateway</html>")
        assertEquals(AuthErrors.SERVER, signIn().message)
    }

    @Test
    fun `network failures are explained without exception text`() = runBlocking<Unit> {
        whenever(apiService.login(any())).thenAnswer { throw UnknownHostException("Unable to resolve host \"api\"") }
        assertEquals(AuthErrors.NO_CONNECTION, signIn().message)

        whenever(apiService.login(any())).thenAnswer { throw SocketTimeoutException("timeout") }
        assertEquals(AuthErrors.TIMEOUT, signIn().message)

        whenever(apiService.login(any())).thenAnswer { throw IllegalStateException("boom") }
        assertEquals(AuthErrors.SERVER, signIn().message)

        verify(tokenManager, never()).saveSession(any(), any(), any(), any())
    }

    @Test
    fun `screens that open together share one profile request`() = runBlocking<Unit> {
        whenever(tokenManager.getIdToken()).thenReturn("i")
        whenever(tokenManager.getUserId()).thenReturn("u1")
        whenever(apiService.getCurrentUser("Bearer i")).thenReturn(
            Response.success(UserDto(id = "u1", email = "chv@example.org", name = "Amina", manualStatus = "active"))
        )

        val home = repository.getCurrentUser().toList().last()
        val profile = repository.getCurrentUser().toList().last()
        val settings = repository.getCurrentUser().toList().last()

        assertEquals("Amina", home.data?.name)
        assertEquals("Amina", profile.data?.name)
        assertEquals("Amina", settings.data?.name)
        verify(apiService, times(1)).getCurrentUser(any())
    }

    @Test
    fun `clocking in or out refreshes the profile next time`() = runBlocking<Unit> {
        whenever(tokenManager.getIdToken()).thenReturn("i")
        whenever(tokenManager.getUserId()).thenReturn("u1")
        whenever(apiService.getCurrentUser("Bearer i")).thenReturn(Response.success(UserDto(id = "u1", name = "Amina")))
        whenever(apiService.clockAction(any(), any())).thenReturn(
            Response.success(com.afyaquest.app.data.remote.dto.ClockActionResponse("ok", "active", "2026-11-12T08:00:00Z"))
        )

        repository.getCurrentUser().toList()
        assertEquals("active", repository.clockAction("clock_in").toList().last().data)
        repository.getCurrentUser().toList()

        // The cached profile carried the old clock status, so it was fetched again.
        verify(apiService, times(2)).getCurrentUser(any())
    }

    @Test
    fun `the profile is not fetched when nobody is signed in`() = runBlocking<Unit> {
        whenever(tokenManager.getIdToken()).thenReturn(null)

        val result = repository.getCurrentUser().toList().last()

        assertTrue(result is Resource.Error)
        assertNull(result.data)
        verify(apiService, never()).getCurrentUser(any())
    }
}
