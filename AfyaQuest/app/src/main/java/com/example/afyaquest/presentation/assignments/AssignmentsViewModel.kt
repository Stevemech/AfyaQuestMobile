package com.example.afyaquest.presentation.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.afyaquest.data.remote.dto.AssignmentDto
import com.example.afyaquest.data.repository.AssignmentsRepository
import com.example.afyaquest.sync.VideoDownloadManager
import com.example.afyaquest.util.ProgressDataStore
import com.example.afyaquest.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val moduleVideoUrls = mapOf(
    "video-8" to "https://afyaquest-module-videos.s3.af-south-1.amazonaws.com/Male+Reproductive+System+(1).mp4",
    "video-9" to "https://afyaquest-module-videos.s3.af-south-1.amazonaws.com/Female+Reproductive+System.mp4",
    "video-10" to "https://afyaquest-module-videos.s3.af-south-1.amazonaws.com/Urinary+System.mov"
)

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

    // Local completion sets — used to override API status immediately
    private val _completedLessons = MutableStateFlow<Set<String>>(emptySet())
    private val _completedQuizzes = MutableStateFlow<Set<String>>(emptySet())
    private val _watchedVideos = MutableStateFlow<Set<String>>(emptySet())

    init {
        videoDownloadManager.refreshDownloadedState()
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

    fun loadAssignments() {
        viewModelScope.launch {
            assignmentsRepository.getAssignments().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val assignments = resource.data?.assignments ?: emptyList()
                        Log.d("AssignmentsVM", "Loaded ${assignments.size} assignments from API")
                        _assignmentsState.value = Resource.Success(assignments)
                        queueModuleDownloads(assignments)
                    }
                    is Resource.Error -> {
                        _assignmentsState.value = Resource.Error(
                            resource.message ?: "Failed to load assignments"
                        )
                    }
                    is Resource.Loading -> {
                        _assignmentsState.value = Resource.Loading()
                    }
                }
            }
        }
    }

    private fun queueModuleDownloads(assignments: List<AssignmentDto>) {
        val assignedModuleIds = assignments
            .filter { it.type == "module" && it.moduleId != null }
            .mapNotNull { it.moduleId }
        videoDownloadManager.queueAssignedModuleDownloads(assignedModuleIds, moduleVideoUrls)
    }

    fun setFilter(filter: AssignmentFilter) {
        _selectedFilter.value = filter
    }

    /**
     * Get assignments with local completion status merged in.
     * If the user completed a lesson/module locally, show it as "completed"
     * even before the API re-fetches.
     */
    fun getFilteredAssignments(): List<AssignmentDto> {
        val all = (_assignmentsState.value as? Resource.Success)?.data ?: emptyList()
        val merged = all.map { assignment ->
            val locallyCompleted = when (assignment.type) {
                "lesson" -> assignment.lessonId != null && _completedLessons.value.contains(assignment.lessonId)
                "module" -> {
                    val mid = assignment.moduleId
                    mid != null && _completedQuizzes.value.contains(mid)
                }
                else -> false
            }
            if (locallyCompleted && assignment.status != "completed") {
                assignment.copy(status = "completed")
            } else {
                assignment
            }
        }
        return when (_selectedFilter.value) {
            AssignmentFilter.ALL -> merged
            AssignmentFilter.MANDATORY -> merged.filter { it.mandatory }
            AssignmentFilter.MODULES -> merged.filter { it.type == "module" }
            AssignmentFilter.LESSONS -> merged.filter { it.type == "lesson" }
            AssignmentFilter.REPORTS -> merged.filter { it.type == "report" }
        }
    }

    fun getMandatoryCount(): Int {
        val all = (_assignmentsState.value as? Resource.Success)?.data ?: emptyList()
        return all.count { it.mandatory }
    }

    fun getPendingCount(): Int {
        val all = getFilteredAssignments()
        return all.count { it.status != "completed" }
    }
}

enum class AssignmentFilter {
    ALL, MANDATORY, MODULES, LESSONS, REPORTS
}
