package com.afyaquest.app.presentation.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
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
        val videoUrls = VideoModulesViewModel.allVideoUrls()
        val assignedModuleIds = assignments
            .filter { (it.type == "module" || it.type == "video") && !it.moduleId.isNullOrBlank() }
            .mapNotNull { it.moduleId }
        videoDownloadManager.queueAssignedModuleDownloads(assignedModuleIds, videoUrls)
    }

    fun setFilter(filter: AssignmentFilter) {
        _selectedFilter.value = filter
    }

    fun getFilteredAssignments(): List<AssignmentDto> {
        val all = (_assignmentsState.value as? Resource.Success)?.data ?: emptyList()
        val merged = all.map { assignment ->
            val t = assignment.type
            val locallyCompleted = when {
                t == "lesson" -> assignment.lessonId != null && _completedLessons.value.contains(assignment.lessonId)
                t == "module" || t == "video" -> {
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
            AssignmentFilter.MODULES -> merged.filter { it.type == "module" || it.type == "video" }
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
