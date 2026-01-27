package com.example.afyaquest.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afyaquest.sync.SyncManager
import com.example.afyaquest.util.NetworkMonitor
import com.example.afyaquest.util.XpData
import com.example.afyaquest.util.XpManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val syncManager: SyncManager
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

    init {
        // Initialize lives if needed
        viewModelScope.launch {
            xpManager.initializeLivesIfNeeded()
        }
    }

    /**
     * Trigger immediate sync
     */
    fun triggerSync() {
        syncManager.triggerImmediateSync()
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
