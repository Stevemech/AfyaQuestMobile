package com.afyaquest.app.presentation.assignments

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afyaquest.app.data.remote.dto.AssignmentDto
import com.afyaquest.app.data.repository.AssignmentsRepository
import com.afyaquest.app.presentation.videomodules.VideoModulesViewModel
import com.afyaquest.app.sync.VideoDownloadManager
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Screen model for the Tasks tab. Everything is derived from ONE merged list
 * (API status + local completion) so the stats and the rows never disagree.
 */
data class AssignmentsUiState(
    /** Merged, unfiltered list. */
    val all: List<AssignmentDto> = emptyList(),
    /** Merged list after the selected filter, sorted for display. */
    val visible: List<AssignmentDto> = emptyList()
) {
    val total: Int get() = all.size
    val completed: Int get() = all.count { it.status == "completed" }
    val pending: Int get() = total - completed
    val mandatory: Int get() = all.count { it.mandatory }
}

@HiltViewModel
class AssignmentsViewModel @Inject constructor(
    private val assignmentsRepository: AssignmentsRepository,
    private val videoDownloadManager: VideoDownloadManager,
    private val progressDataStore: ProgressDataStore
) : ViewModel() {

    private val _assignmentsState = MutableStateFlow<Resource<List<AssignmentDto>>?>(null)
    val assignmentsState: StateFlow<Resource<List<AssignmentDto>>?> = _assignmentsState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(AssignmentFilter.ALL)
    val selectedFilter: StateFlow<AssignmentFilter> = _selectedFilter.asStateFlow()

    private val _completedLessons = MutableStateFlow<Set<String>>(emptySet())
    private val _completedQuizzes = MutableStateFlow<Set<String>>(emptySet())
    private val _watchedVideos = MutableStateFlow<Set<String>>(emptySet())

    /**
     * Reactive merged view: recomputed whenever the API result, the filter, or local
     * lesson/quiz completion changes, so a task flips to "Completed" the moment it is done.
     */
    val uiState: StateFlow<AssignmentsUiState> =
        combine(_assignmentsState, _selectedFilter, _completedLessons, _completedQuizzes) {
                state, filter, lessons, quizzes ->
            buildUiState(state, filter, lessons, quizzes)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssignmentsUiState())

    private var loadJob: Job? = null

    /** When the list was last confirmed by the server ([SystemClock.elapsedRealtime]). */
    private var loadedAt = 0L

    init {
        // Listing the download folder is disk work: not on the main thread.
        viewModelScope.launch(Dispatchers.IO) { videoDownloadManager.refreshDownloadedState() }
        loadLocalProgress()
        loadAssignments()
    }

    private fun loadLocalProgress() {
        viewModelScope.launch {
            progressDataStore.getCompletedLessons().collect { _completedLessons.value = it }
        }
        viewModelScope.launch {
            progressDataStore.getCompletedQuizzes().collect { _completedQuizzes.value = it }
        }
        viewModelScope.launch {
            progressDataStore.getWatchedVideos().collect { _watchedVideos.value = it }
        }
    }

    /**
     * Load the assignments (also Retry and the refresh button). A list that is already on screen
     * stays there while the server is asked again: replacing it with a spinner made the section
     * collapse and everything below it jump every time the screen came back.
     */
    fun loadAssignments() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            assignmentsRepository.getAssignments().collect { resource ->
                val showing = _assignmentsState.value is Resource.Success
                when (resource) {
                    is Resource.Success -> {
                        val assignments = resource.data?.assignments ?: emptyList()
                        _assignmentsState.value = Resource.Success(assignments)
                        loadedAt = SystemClock.elapsedRealtime()
                        queueModuleDownloads(assignments)
                    }
                    is Resource.Error -> if (!showing) {
                        _assignmentsState.value = Resource.Error(resource.message ?: "Failed to load assignments")
                    }
                    is Resource.Loading -> if (!showing) _assignmentsState.value = Resource.Loading()
                }
            }
        }
    }

    /** Ask the server again when the list on screen is older than [STALE_AFTER_MS]. */
    fun refreshIfStale() {
        if (loadJob?.isActive == true) return
        if (SystemClock.elapsedRealtime() - loadedAt > STALE_AFTER_MS) loadAssignments()
    }

    /**
     * Keep the videos of assigned modules on the phone for offline use. Tasks that are already
     * done are skipped: their video is removed a day after the quiz, and without this check it
     * was downloaded again on the next visit to Home, over and over.
     */
    private fun queueModuleDownloads(assignments: List<AssignmentDto>) {
        viewModelScope.launch(Dispatchers.IO) {
            val finished = progressDataStore.getCompletedQuizzes().first()
            val assignedModuleIds = assignments
                .filter { (it.type == "module" || it.type == "video") && !it.moduleId.isNullOrBlank() }
                .filter { it.status != "completed" && it.moduleId !in finished }
                .mapNotNull { it.moduleId }
            videoDownloadManager.queueAssignedModuleDownloads(assignedModuleIds, VideoModulesViewModel.allVideoUrls())
        }
    }

    private companion object {
        const val STALE_AFTER_MS = 60_000L
    }

    fun setFilter(filter: AssignmentFilter) {
        _selectedFilter.value = filter
    }

    // ------------------------------------------------------------------
    // Merge / filter / sort
    // ------------------------------------------------------------------

    private fun mergeWithLocalProgress(
        all: List<AssignmentDto>,
        lessons: Set<String>,
        quizzes: Set<String>
    ): List<AssignmentDto> = all.map { assignment ->
        val t = assignment.type
        val locallyCompleted = when {
            t == "lesson" -> assignment.lessonId != null && lessons.contains(assignment.lessonId)
            t == "module" || t == "video" -> {
                val mid = assignment.moduleId
                mid != null && quizzes.contains(mid)
            }
            else -> false
        }
        if (locallyCompleted && assignment.status != "completed") {
            assignment.copy(status = "completed")
        } else {
            assignment
        }
    }

    private fun applyFilter(merged: List<AssignmentDto>, filter: AssignmentFilter): List<AssignmentDto> =
        when (filter) {
            AssignmentFilter.ALL -> merged
            AssignmentFilter.MANDATORY -> merged.filter { it.mandatory }
            AssignmentFilter.MODULES -> merged.filter { it.type == "module" || it.type == "video" }
            AssignmentFilter.LESSONS -> merged.filter { it.type == "lesson" }
            AssignmentFilter.REPORTS -> merged.filter { it.type == "report" }
        }

    private fun buildUiState(
        state: Resource<List<AssignmentDto>>?,
        filter: AssignmentFilter,
        lessons: Set<String>,
        quizzes: Set<String>
    ): AssignmentsUiState {
        val raw = (state as? Resource.Success)?.data ?: emptyList()
        val merged = mergeWithLocalProgress(raw, lessons, quizzes)
        return AssignmentsUiState(
            all = merged,
            visible = applyFilter(merged, filter).sortedWith(displayOrder)
        )
    }

    // ------------------------------------------------------------------
    // Snapshot helpers kept for callers outside this screen (e.g. Dashboard)
    // ------------------------------------------------------------------

    /** Merged + filtered snapshot of the current state (non-reactive). */
    fun getFilteredAssignments(): List<AssignmentDto> =
        buildUiState(
            _assignmentsState.value,
            _selectedFilter.value,
            _completedLessons.value,
            _completedQuizzes.value
        ).visible

    fun getMandatoryCount(): Int {
        val all = (_assignmentsState.value as? Resource.Success)?.data ?: emptyList()
        return all.count { it.mandatory }
    }

    fun getPendingCount(): Int {
        val all = getFilteredAssignments()
        return all.count { it.status != "completed" }
    }
}

/** Pending before completed; among pending, mandatory first, then earliest due date. */
private val displayOrder: Comparator<AssignmentDto> =
    compareBy<AssignmentDto> { it.status == "completed" }
        .thenByDescending { it.mandatory }
        .thenBy { it.dueDate ?: "9999-12-31" }

enum class AssignmentFilter {
    ALL, MANDATORY, MODULES, LESSONS, REPORTS
}
