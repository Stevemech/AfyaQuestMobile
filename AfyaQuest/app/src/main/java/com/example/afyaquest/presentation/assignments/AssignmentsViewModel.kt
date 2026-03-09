package com.example.afyaquest.presentation.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.afyaquest.data.remote.dto.AssignmentDto
import com.example.afyaquest.data.repository.AssignmentsRepository
import com.example.afyaquest.sync.VideoDownloadManager
import com.example.afyaquest.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Known S3 video URLs for modules — used to trigger downloads for assigned modules.
 */
private val moduleVideoUrls = mapOf(
    "video-8" to "https://afyaquest-module-videos.s3.af-south-1.amazonaws.com/Male+Reproductive+System+(1).mp4",
    "video-9" to "https://afyaquest-module-videos.s3.af-south-1.amazonaws.com/Female+Reproductive+System.mp4",
    "video-10" to "https://afyaquest-module-videos.s3.af-south-1.amazonaws.com/Urinary+System.mov"
)

@HiltViewModel
class AssignmentsViewModel @Inject constructor(
    private val assignmentsRepository: AssignmentsRepository,
    private val videoDownloadManager: VideoDownloadManager
) : ViewModel() {

    private val _assignmentsState = MutableStateFlow<Resource<List<AssignmentDto>>?>(null)
    val assignmentsState: StateFlow<Resource<List<AssignmentDto>>?> = _assignmentsState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(AssignmentFilter.ALL)
    val selectedFilter: StateFlow<AssignmentFilter> = _selectedFilter.asStateFlow()

    init {
        videoDownloadManager.refreshDownloadedState()
        loadAssignments()
    }

    fun loadAssignments() {
        viewModelScope.launch {
            assignmentsRepository.getAssignments().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val assignments = resource.data?.assignments ?: emptyList()
                        Log.d("AssignmentsVM", "Loaded ${assignments.size} assignments:")
                        assignments.forEach { a ->
                            Log.d("AssignmentsVM", "  type=${a.type} moduleId=${a.moduleId} lessonId=${a.lessonId} status=${a.status}")
                        }
                        _assignmentsState.value = Resource.Success(assignments)

                        // Auto-queue downloads for assigned video modules
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

    /**
     * Automatically queue video downloads for any assigned modules.
     * WorkManager handles the network constraint — downloads start
     * once a good connection is established.
     */
    private fun queueModuleDownloads(assignments: List<AssignmentDto>) {
        val assignedModuleIds = assignments
            .filter { it.type == "module" && it.moduleId != null }
            .mapNotNull { it.moduleId }

        videoDownloadManager.queueAssignedModuleDownloads(assignedModuleIds, moduleVideoUrls)
    }

    fun setFilter(filter: AssignmentFilter) {
        _selectedFilter.value = filter
    }

    fun getFilteredAssignments(): List<AssignmentDto> {
        val all = (_assignmentsState.value as? Resource.Success)?.data ?: emptyList()
        return when (_selectedFilter.value) {
            AssignmentFilter.ALL -> all
            AssignmentFilter.MANDATORY -> all.filter { it.mandatory }
            AssignmentFilter.MODULES -> all.filter { it.type == "module" }
            AssignmentFilter.LESSONS -> all.filter { it.type == "lesson" }
            AssignmentFilter.REPORTS -> all.filter { it.type == "report" }
        }
    }

    fun getMandatoryCount(): Int {
        val all = (_assignmentsState.value as? Resource.Success)?.data ?: emptyList()
        return all.count { it.mandatory }
    }

    fun getPendingCount(): Int {
        val all = (_assignmentsState.value as? Resource.Success)?.data ?: emptyList()
        return all.count { it.status != "completed" }
    }
}

enum class AssignmentFilter {
    ALL, MANDATORY, MODULES, LESSONS, REPORTS
}
