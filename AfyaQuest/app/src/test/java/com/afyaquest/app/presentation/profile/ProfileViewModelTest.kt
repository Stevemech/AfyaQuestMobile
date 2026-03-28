package com.afyaquest.app.presentation.profile

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.data.repository.AuthRepository
import com.afyaquest.app.domain.model.AchievementCategory
import com.afyaquest.app.util.LanguageManager
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.Resource
import com.afyaquest.app.util.TokenManager
import com.afyaquest.app.util.XpManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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

        whenever(authRepository.getCurrentUser()).thenReturn(flowOf(Resource.Loading()))

        viewModel = ProfileViewModel(progressDataStore, apiService, tokenManager, xpManager, languageManager, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `achievements list is populated on init`() {
        val achievements = viewModel.achievements.value

        assertTrue(achievements.isNotEmpty())
        assertEquals(8, achievements.size)
    }

    @Test
    fun `achievements contain correct categories`() {
        val achievements = viewModel.achievements.value

        val categories = achievements.map { it.category }.toSet()
        assertTrue(categories.contains(AchievementCategory.LEARNING))
        assertTrue(categories.contains(AchievementCategory.CONSISTENCY))
        assertTrue(categories.contains(AchievementCategory.COMMUNITY))
    }

    @Test
    fun `some achievements are unlocked`() {
        val achievements = viewModel.achievements.value

        val unlockedCount = achievements.count { it.unlocked }
        assertTrue(unlockedCount > 0)
    }

    @Test
    fun `some achievements are locked`() {
        val achievements = viewModel.achievements.value

        val lockedCount = achievements.count { !it.unlocked }
        assertTrue(lockedCount > 0)
    }

    @Test
    fun `weekly reflections list is populated`() {
        val reflections = viewModel.weeklyReflections.value

        assertTrue(reflections.isNotEmpty())
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
}
