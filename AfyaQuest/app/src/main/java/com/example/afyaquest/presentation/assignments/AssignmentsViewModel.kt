package com.example.afyaquest.presentation.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afyaquest.data.remote.dto.AssignmentDto
import com.example.afyaquest.data.repository.AssignmentsRepository
import com.example.afyaquest.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssignmentsViewModel @Inject constructor(
    private val assignmentsRepository: AssignmentsRepository
) : ViewModel() {

    private val _assignmentsState = MutableStateFlow<Resource<List<AssignmentDto>>?>(null)
    val assignmentsState: StateFlow<Resource<List<AssignmentDto>>?> = _assignmentsState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(AssignmentFilter.ALL)
    val selectedFilter: StateFlow<AssignmentFilter> = _selectedFilter.asStateFlow()

    init {
        loadAssignments()
    }

    fun loadAssignments() {
        viewModelScope.launch {
            assignmentsRepository.getAssignments().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _assignmentsState.value = Resource.Success(
                            resource.data?.assignments ?: emptyList()
                        )
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
