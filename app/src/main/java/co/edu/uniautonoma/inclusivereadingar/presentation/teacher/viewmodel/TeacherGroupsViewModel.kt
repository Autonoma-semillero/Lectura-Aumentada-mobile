package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.GroupsDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceStudent
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentGroup
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeacherGroupsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val groups: List<StudentGroup> = emptyList(),
    val students: List<AudienceStudent> = emptyList(),
    val showEditor: Boolean = false,
    val editingGroup: StudentGroup? = null,
    val archiveConfirmGroup: StudentGroup? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val unassignedStudents: List<AudienceStudent>
        get() = students.filter { it.unassigned || it.groupIds.isEmpty() }
}

class TeacherGroupsViewModel(
    private val repository: GroupsDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeacherGroupsUiState())
    val uiState: StateFlow<TeacherGroupsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val groups = async { repository.getGroups(status = "active") }
                val audience = async { repository.searchAudience(q = "", limit = 100) }
                groups.await() to audience.await().items.filterIsInstance<AudienceStudent>()
            }.onSuccess { (groups, students) ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    groups = groups.sortedBy { it.name.lowercase() },
                    students = students.sortedBy { it.label.lowercase() },
                    errorMessage = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.toUiMessage("No fue posible cargar los grupos.")
                )
            }
        }
    }

    fun openCreate() {
        _uiState.value = _uiState.value.copy(showEditor = true, editingGroup = null)
    }

    fun openEdit(group: StudentGroup) {
        _uiState.value = _uiState.value.copy(showEditor = true, editingGroup = group)
    }

    fun dismissEditor() {
        if (_uiState.value.isSaving) return
        _uiState.value = _uiState.value.copy(showEditor = false, editingGroup = null)
    }

    fun saveGroup(name: String, description: String?, desiredStudentIds: Set<String>) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Escribe un nombre para el grupo.")
            return
        }
        val current = _uiState.value.editingGroup
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                if (current == null) {
                    repository.createGroup(name.trim(), description, desiredStudentIds)
                } else {
                    repository.updateGroup(current.id, name.trim(), description)
                    val currentIds = current.studentIds.toSet()
                    val toAdd = desiredStudentIds - currentIds
                    val toRemove = currentIds - desiredStudentIds
                    if (toAdd.isNotEmpty()) repository.addMembers(current.id, toAdd)
                    toRemove.forEach { repository.removeMember(current.id, it) }
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    showEditor = false,
                    editingGroup = null,
                    successMessage = if (current == null) "Grupo creado." else "Grupo actualizado."
                )
                load()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = error.toUiMessage("No fue posible guardar el grupo.")
                )
            }
        }
    }

    fun requestArchive(group: StudentGroup) {
        _uiState.value = _uiState.value.copy(archiveConfirmGroup = group)
    }

    fun cancelArchive() {
        _uiState.value = _uiState.value.copy(archiveConfirmGroup = null)
    }

    fun confirmArchive() {
        val group = _uiState.value.archiveConfirmGroup ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, archiveConfirmGroup = null)
            runCatching { repository.archiveGroup(group.id) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        successMessage = "Grupo archivado. Los estudiantes quedaron disponibles."
                    )
                    load()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = error.toUiMessage("No fue posible archivar el grupo.")
                    )
                }
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}

class TeacherGroupsViewModelFactory(
    private val repository: GroupsDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherGroupsViewModel::class.java)) {
            return TeacherGroupsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
