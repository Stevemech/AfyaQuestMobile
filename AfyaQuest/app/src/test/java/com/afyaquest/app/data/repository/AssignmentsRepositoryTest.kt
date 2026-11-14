package com.afyaquest.app.data.repository

import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.data.remote.dto.AssignmentDto
import com.afyaquest.app.data.remote.dto.AssignmentsResponse
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.Resource
import com.afyaquest.app.util.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.IOException

/** Assignments: the copy saved on the phone is shown first, the server's answer replaces it. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AssignmentsRepositoryTest {

    @Mock private lateinit var apiService: ApiService
    @Mock private lateinit var tokenManager: TokenManager
    @Mock private lateinit var progressDataStore: ProgressDataStore

    private lateinit var repository: AssignmentsRepository

    private val lesson = AssignmentDto(type = "lesson", lessonId = "lesson-8", status = "assigned", mandatory = true, assignedAt = "2026-11-12T08:00:00Z")
    private val module = AssignmentDto(type = "module", moduleId = "mod2-malnutrition", status = "assigned", assignedAt = "2026-11-12T08:00:00Z")

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(tokenManager.getIdToken()).thenReturn("token")
        repository = AssignmentsRepository(apiService, tokenManager, progressDataStore)
    }

    private fun saved(vararg items: AssignmentDto) = runBlocking<Unit> {
        whenever(progressDataStore.getCachedAssignments()).thenReturn(Gson().toJson(items.toList()))
    }

    private fun server(vararg items: AssignmentDto) = runBlocking<Unit> {
        whenever(apiService.getAssignments("Bearer token")).thenReturn(Response.success(AssignmentsResponse(items.toList())))
    }

    private fun emissions() = runBlocking { repository.getAssignments().toList() }

    @Test
    fun `the saved copy is shown first and the server's list replaces it`() = runBlocking<Unit> {
        saved(lesson)
        server(lesson, module)

        val seen = emissions()

        // No "loading" state at all: there was something to show from the first moment.
        assertEquals(2, seen.size)
        assertEquals(listOf(lesson), (seen[0] as Resource.Success).data?.assignments)
        assertEquals(listOf(lesson, module), (seen[1] as Resource.Success).data?.assignments)
        verify(progressDataStore).cacheAssignments(any())
    }

    @Test
    fun `nothing is emitted twice when the server agrees with the saved copy`() = runBlocking<Unit> {
        saved(lesson, module)
        server(lesson, module)

        val seen = emissions()

        assertEquals(1, seen.size)
        verify(progressDataStore, never()).cacheAssignments(any())
    }

    @Test
    fun `without a connection the saved copy stays and no error is shown`() = runBlocking<Unit> {
        saved(lesson)
        whenever(apiService.getAssignments(any())).thenAnswer { throw IOException("offline") }

        val seen = emissions()

        assertEquals(1, seen.size)
        assertEquals(listOf(lesson), (seen[0] as Resource.Success).data?.assignments)
    }

    @Test
    fun `a first load shows loading, then the list`() = runBlocking<Unit> {
        whenever(progressDataStore.getCachedAssignments()).thenReturn(null)
        server(lesson)

        val seen = emissions()

        assertTrue(seen[0] is Resource.Loading)
        assertEquals(listOf(lesson), (seen[1] as Resource.Success).data?.assignments)
    }

    @Test
    fun `an error is only reported when there is nothing saved either`() = runBlocking<Unit> {
        whenever(progressDataStore.getCachedAssignments()).thenReturn(null)
        whenever(apiService.getAssignments(any())).thenAnswer { throw IOException("offline") }

        val seen = emissions()

        assertTrue(seen[0] is Resource.Loading)
        assertTrue(seen[1] is Resource.Error)
    }

    @Test
    fun `rows without an assignment date are dropped`() = runBlocking<Unit> {
        whenever(progressDataStore.getCachedAssignments()).thenReturn(null)
        server(lesson, AssignmentDto(type = "module", moduleId = "ghost", status = "in_progress"))

        assertEquals(listOf(lesson), (emissions().last() as Resource.Success).data?.assignments)
    }
}
