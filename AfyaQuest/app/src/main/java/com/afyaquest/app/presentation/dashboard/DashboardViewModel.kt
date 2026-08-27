package com.afyaquest.app.presentation.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afyaquest.app.data.repository.AuthRepository
import com.afyaquest.app.presentation.lessons.LessonsViewModel
import com.afyaquest.app.data.repository.ReportsRepository
import com.afyaquest.app.presentation.videomodules.VideoModulesViewModel
import com.afyaquest.app.sync.SyncManager
import com.afyaquest.app.util.NetworkMonitor
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.Resource
import com.afyaquest.app.util.XpData
import com.afyaquest.app.util.XpManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * "What do I still need to do today?" for the three daily tasks shown on the Home hub.
 */
data class DailyProgress(
    val questionsDone: Boolean = false,
    /** Correct answers of today's daily-questions session, null until finished today. */
    val questionsCorrect: Int? = null,
    val questionsTotal: Int? = null,
    val stopsVisited: Int = 0,
    /** 0 until today's itinerary has been loaded once. */
    val stopsTotal: Int = 0,
    val reportDone: Boolean = false
) {
    val itineraryDone: Boolean get() = stopsTotal > 0 && stopsVisited >= stopsTotal
    val doneCount: Int get() = listOf(questionsDone, itineraryDone, reportDone).count { it }
    val allDone: Boolean get() = doneCount >= TOTAL_TASKS

    companion object {
        const val TOTAL_TASKS = 3
    }
}

/** Overall learning progress shown on the Learning Center cards. */
data class LearningProgress(
    val videosWatched: Int = 0,
    val videosTotal: Int = 0,
    val lessonsCompleted: Int = 0,
    val lessonsTotal: Int = 0
)

/**
 * ViewModel for Dashboard screen
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val xpManager: XpManager,
    private val networkMonitor: NetworkMonitor,
    private val syncManager: SyncManager,
    private val authRepository: AuthRepository,
    private val progressDataStore: ProgressDataStore,
    private val reportsRepository: ReportsRepository
) : ViewModel() {

    companion object {
        private const val TOTAL_LESSONS = LessonsViewModel.TOTAL_LESSONS
    }

    // Daily To-Do progress (questions / itinerary / report), all date-scoped to today
    val dailyProgress: StateFlow<DailyProgress> = combine(
        progressDataStore.isDailyQuestionsDoneToday(),
        progressDataStore.getDailyQuestionsResult(),
        progressDataStore.getItineraryProgressToday(),
        reportsRepository.observeReportSubmittedToday()
    ) { questionsDone, questionsResult, itinerary, reportDone ->
        DailyProgress(
            questionsDone = questionsDone,
            questionsCorrect = if (questionsDone) questionsResult?.correctAnswers else null,
            questionsTotal = if (questionsDone) questionsResult?.totalQuestions else null,
            stopsVisited = itinerary.visited,
            stopsTotal = itinerary.total,
            reportDone = reportDone
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyProgress()
    )

    // Learning Center progress (videos watched / lessons completed)
    val learningProgress: StateFlow<LearningProgress> = combine(
        progressDataStore.getWatchedVideos(),
        progressDataStore.getCompletedLessons()
    ) { watched, lessons ->
        val videosTotal = VideoModulesViewModel.allVideos().size
        LearningProgress(
            videosWatched = watched.size.coerceAtMost(videosTotal),
            videosTotal = videosTotal,
            lessonsCompleted = lessons.size.coerceAtMost(TOTAL_LESSONS),
            lessonsTotal = TOTAL_LESSONS
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LearningProgress(
            videosTotal = VideoModulesViewModel.allVideos().size,
            lessonsTotal = TOTAL_LESSONS
        )
    )

    // XP data flow
    val xpData: StateFlow<XpData> = xpManager.getXpDataFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = XpData()
        )

    // Network connectivity flow
    val isConnected: StateFlow<Boolean> = networkMonitor.isConnected
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    // Unsynced items count flow
    val unsyncedCount: StateFlow<Int> = syncManager.totalUnsyncedCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Sync in-progress indicator
    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing

    // Sync error message
    val syncError: StateFlow<String?> = syncManager.lastSyncError

    // Clock-in status
    private val _isClockActive = MutableStateFlow(false)
    val isClockActive: StateFlow<Boolean> = _isClockActive.asStateFlow()

    private val _clockLoading = MutableStateFlow(false)
    val clockLoading: StateFlow<Boolean> = _clockLoading.asStateFlow()

    private val _clockError = MutableStateFlow<String?>(null)
    val clockError: StateFlow<String?> = _clockError.asStateFlow()

    init {
        // Initialize lives if needed
        viewModelScope.launch {
            xpManager.initializeLivesIfNeeded()
        }

        // Sync XP data from server so local DataStore reflects server state
        viewModelScope.launch {
            authRepository.getCurrentUser().collectLatest { result ->
                if (result is Resource.Success) {
                    val user = result.data ?: return@collectLatest
                    xpManager.syncFromServer(
                        totalXP = user.totalPoints,
                        streak = user.currentStreak,
                        level = user.level,
                        rank = user.rank
                    )
                    _isClockActive.value = user.isActive
                    Log.d("DashboardVM", "Synced XP from server: xp=${user.totalPoints}, level=${user.level}")
                }
            }
        }

        // Auto-sync when network becomes available and there are unsynced items
        viewModelScope.launch {
            networkMonitor.isConnected
                .distinctUntilChanged()
                .collectLatest { connected ->
                    if (connected && unsyncedCount.value > 0) {
                        syncManager.syncNow()
                    }
                }
        }
    }

    /**
     * Trigger immediate sync directly (not via WorkManager)
     */
    fun triggerSync() {
        viewModelScope.launch {
            syncManager.syncNow()
        }
    }

    /**
     * Toggle clock in/out status.
     */
    fun toggleClockStatus() {
        viewModelScope.launch {
            _clockLoading.value = true
            _clockError.value = null
            val action = if (_isClockActive.value) "clock_out" else "clock_in"
            authRepository.clockAction(action).collectLatest { result ->
                when (result) {
                    is Resource.Success -> {
                        _isClockActive.value = result.data == "active"
                        _clockLoading.value = false
                    }
                    is Resource.Error -> {
                        _clockError.value = result.message
                        _clockLoading.value = false
                    }
                    is Resource.Loading -> { /* loading already set */ }
                }
            }
        }
    }

    fun dismissClockError() {
        _clockError.value = null
    }

    /**
     * Get XP needed for next level
     */
    fun getXPForNextLevel(): Int {
        val data = xpData.value
        return xpManager.getXPForNextLevel(data.totalXP, data.level)
    }

    /**
     * Get level progress percentage
     */
    fun getLevelProgress(): Float {
        val data = xpData.value
        return xpManager.getLevelProgress(data.totalXP, data.level)
    }
}
