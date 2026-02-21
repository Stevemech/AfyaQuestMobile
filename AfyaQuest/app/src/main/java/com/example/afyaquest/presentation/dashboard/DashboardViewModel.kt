package com.example.afyaquest.presentation.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afyaquest.data.repository.AuthRepository
import com.example.afyaquest.sync.SyncManager
import com.example.afyaquest.util.NetworkMonitor
import com.example.afyaquest.util.Resource
import com.example.afyaquest.util.XpData
import com.example.afyaquest.util.XpManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Dashboard screen
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val xpManager: XpManager,
    private val networkMonitor: NetworkMonitor,
    private val syncManager: SyncManager,
    private val authRepository: AuthRepository
) : ViewModel() {

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
