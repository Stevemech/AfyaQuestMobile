package com.afyaquest.app.presentation.profile

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.data.repository.AuthRepository
import com.afyaquest.app.domain.model.User
import com.afyaquest.app.domain.model.WeeklyReflection
import com.afyaquest.app.util.LanguageManager
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.Resource
import com.afyaquest.app.util.TokenManager
import com.afyaquest.app.util.XpManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

/**
 * Unit tests for ProfileViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var progressDataStore: ProgressDataStore

    @Mock
    private lateinit var apiService: ApiService

    @Mock
    private lateinit var tokenManager: TokenManager

    @Mock
    private lateinit var xpManager: XpManager

    @Mock
    private lateinit var languageManager: LanguageManager

    @Mock
    private lateinit var authRepository: AuthRepository

    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(xpManager.getXpDataFlow()).thenReturn(
            flowOf(com.afyaquest.app.util.XpData(
                totalXP = 500,
                dailyXP = 100,
                level = 2,
                lives = 5,
                streak = 7,
                rank = "Novice Helper"
            ))
        )

        whenever(languageManager.getCurrentLanguageFlow()).thenReturn(
            flowOf(LanguageManager.LANGUAGE_ENGLISH)
        )

        whenever(progressDataStore.getWatchedVideos()).thenReturn(flowOf(emptySet()))
        whenever(progressDataStore.getCompletedLessons()).thenReturn(flowOf(emptySet()))
        whenever(progressDataStore.getCompletedQuizzes()).thenReturn(flowOf(emptySet()))
        whenever(progressDataStore.getReportDates()).thenReturn(flowOf(emptySet()))

        whenever(authRepository.getCurrentUser()).thenReturn(flowOf(Resource.Loading()))
        whenever(progressDataStore.clockedIn()).thenReturn(flowOf(false))

        viewModel = ProfileViewModel(progressDataStore, apiService, tokenManager, xpManager, languageManager, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Achievements are worked out from the account's real progress (see AchievementRulesTest for
    // the rules themselves); here: that the ViewModel feeds them the stored progress.

    @Test
    fun `a new account shows every achievement locked`() = runTest(testDispatcher) {
        val achievements = collected(viewModel.achievements)

        assertEquals(AchievementRules.definitions.size, achievements.size)
        assertTrue(achievements.none { it.unlocked })
    }

    @Test
    fun `achievements follow the stored progress`() = runTest(testDispatcher) {
        whenever(progressDataStore.getWatchedVideos()).thenReturn(flowOf(setOf("mod1-female-reproductive")))
        whenever(progressDataStore.getCompletedQuizzes()).thenReturn(flowOf(emptySet()))
        whenever(progressDataStore.getCompletedLessons()).thenReturn(flowOf(setOf("lesson-3")))
        whenever(progressDataStore.getReportDates()).thenReturn(flowOf(setOf("2026-11-11", "2026-11-12")))
        whenever(xpManager.getXpDataFlow()).thenReturn(
            flowOf(com.afyaquest.app.util.XpData(totalXP = 520, level = 3, longestStreak = 3))
        )
        val withProgress = ProfileViewModel(progressDataStore, apiService, tokenManager, xpManager, languageManager, authRepository)

        val unlocked = collected(withProgress.achievements).filter { it.unlocked }.map { it.definition.id }.toSet()

        assertEquals(setOf("first-video", "report-1", "streak-3", "level-3"), unlocked)
    }

    @Test
    fun `quick stats count the days a report was submitted`() = runTest(testDispatcher) {
        whenever(progressDataStore.getReportDates()).thenReturn(flowOf(setOf("2026-11-11", "2026-11-12")))
        val withReports = ProfileViewModel(progressDataStore, apiService, tokenManager, xpManager, languageManager, authRepository)

        assertEquals(2, collected(withReports.quickStats).reportsSubmitted)
    }

    @Test
    fun `the profile saved on the phone shows while the server cannot be reached`() = runTest(testDispatcher) {
        whenever(tokenManager.getUserId()).thenReturn("user-1")
        whenever(authRepository.getLocalUser("user-1")).thenReturn(
            User(id = "user-1", email = "chv@example.org", name = "Amina", organization = "kibera-east")
        )
        whenever(authRepository.getCurrentUser()).thenReturn(flowOf(Resource.Error("Connection error")))
        val offline = ProfileViewModel(progressDataStore, apiService, tokenManager, xpManager, languageManager, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Amina", offline.userProfile.value?.name)
        assertEquals("kibera-east", offline.userProfile.value?.organization)
    }

    @Test
    fun `the server profile replaces the saved copy`() = runTest(testDispatcher) {
        whenever(tokenManager.getUserId()).thenReturn("user-1")
        whenever(authRepository.getLocalUser("user-1")).thenReturn(
            User(id = "user-1", email = "chv@example.org", name = "Old name")
        )
        whenever(authRepository.getCurrentUser()).thenReturn(
            flowOf(Resource.Success(User(id = "user-1", email = "chv@example.org", name = "New name")))
        )
        val online = ProfileViewModel(progressDataStore, apiService, tokenManager, xpManager, languageManager, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("New name", online.userProfile.value?.name)
    }

    @Test
    fun `status follows the clock-in state instead of always saying active`() = runTest(testDispatcher) {
        assertFalse(collected(viewModel.clockedIn))

        whenever(progressDataStore.clockedIn()).thenReturn(flowOf(true))
        val clockedIn = ProfileViewModel(progressDataStore, apiService, tokenManager, xpManager, languageManager, authRepository)

        assertTrue(collected(clockedIn.clockedIn))
    }

    @Test
    fun `organization ids are shown as readable names`() {
        assertEquals("Kibera East", organizationDisplayName("kibera-east"))
        assertEquals("Mathare", organizationDisplayName("mathare"))
        assertEquals("San Juan Comalapa", organizationDisplayName("san_juan-comalapa"))
        assertEquals("", organizationDisplayName(""))
    }

    @Test
    fun `weekly reflections start empty and the newest submission comes first`() {
        assertTrue(viewModel.weeklyReflections.value.isEmpty())

        viewModel.submitReflection(reflection("r1"))
        viewModel.submitReflection(reflection("r2"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("r2", "r1"), viewModel.weeklyReflections.value.map { it.id })
    }

    @Test
    fun `setSelectedTab changes selected tab`() {
        viewModel.setSelectedTab(1)
        assertEquals(1, viewModel.selectedTab.value)

        viewModel.setSelectedTab(2)
        assertEquals(2, viewModel.selectedTab.value)
    }

    @Test
    fun `initial selected tab is 0`() {
        assertEquals(0, viewModel.selectedTab.value)
    }

    /** Current value of a WhileSubscribed StateFlow once its upstream has been collected. */
    private fun <T> kotlinx.coroutines.test.TestScope.collected(flow: kotlinx.coroutines.flow.StateFlow<T>): T {
        val job = launch { flow.collect { } }
        testDispatcher.scheduler.advanceUntilIdle()
        return flow.value.also { job.cancel() }
    }

    private fun reflection(id: String) = WeeklyReflection(
        id = id,
        weekStartDate = "2026-11-02",
        weekEndDate = "2026-11-08",
        successStory = "Visited every household on my route",
        challengesFaced = "Heavy rain",
        lessonsLearned = "Start earlier",
        goalsNextWeek = "Finish the maternal health videos",
        overallRating = 4,
        submittedDate = "2026-11-08"
    )
}
