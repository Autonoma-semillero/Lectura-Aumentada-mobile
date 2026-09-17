package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.GroupsDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceStudent
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentGroup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeacherGroupsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val groups: List<StudentGroup> = emptyList(),
    val unassignedQuery: String = "",
    val unassignedResults: List<AudienceStudent> = emptyList(),
    val isSearchingUnassigned: Boolean = false,
    val editorQuery: String = "",
    val editorResults: List<AudienceStudent> = emptyList(),
    val isSearchingEditor: Boolean = false,
    val knownStudents: Map<String, AudienceStudent> = emptyMap(),
    val showEditor: Boolean = false,
    val editingGroup: StudentGroup? = null,
    val archiveConfirmGroup: StudentGroup? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class TeacherGroupsViewModel(
    private val repository: GroupsDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeacherGroupsUiState())
    val uiState: StateFlow<TeacherGroupsUiState> = _uiState.asStateFlow()
    private var unassignedSearchJob: Job? = null
    private var editorSearchJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val unassignedQuery = _uiState.value.unassignedQuery
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val groups = async { repository.getGroups(status = "active") }
                val audience = async {
                    repository.searchAudience(
                        q = unassignedQuery,
                        limit = 30,
                        unassigned = true
                    )
                }
                groups.await() to audience.await().items.filterIsInstance<AudienceStudent>()
            }.onSuccess { (groups, students) ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    groups = groups.sortedBy { it.name.lowercase() },
                    unassignedResults = students.sortedBy { it.label.lowercase() },
                    knownStudents = _uiState.value.knownStudents + students.associateBy { it.id },
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
        _uiState.value = _uiState.value.copy(
            showEditor = true,
            editingGroup = null,
            editorQuery = "",
            editorResults = emptyList()
        )
        searchEditorStudents("")
    }

    fun openEdit(group: StudentGroup) {
        _uiState.value = _uiState.value.copy(
            showEditor = true,
            editingGroup = group,
            editorQuery = "",
            editorResults = emptyList()
        )
        searchEditorStudents("")
    }

    fun dismissEditor() {
        if (_uiState.value.isSaving) return
        editorSearchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            showEditor = false,
            editingGroup = null,
            editorQuery = "",
            editorResults = emptyList(),
            isSearchingEditor = false
        )
    }

    fun onUnassignedQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            unassignedQuery = query,
            isSearchingUnassigned = true
        )
        unassignedSearchJob?.cancel()
        unassignedSearchJob = viewModelScope.launch {
            delay(250)
            try {
                val students = repository.searchAudience(
                    q = query,
                    limit = 30,
                    unassigned = true
                ).items.filterIsInstance<AudienceStudent>()
                _uiState.value = _uiState.value.copy(
                    isSearchingUnassigned = false,
                    unassignedResults = students,
                    knownStudents = _uiState.value.knownStudents + students.associateBy { it.id }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isSearchingUnassigned = false,
                    errorMessage = error.toUiMessage("No fue posible buscar estudiantes sin grupo.")
                )
            }
        }
    }

    fun onEditorQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(editorQuery = query)
        searchEditorStudents(query)
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

    private fun searchEditorStudents(query: String) {
        _uiState.value = _uiState.value.copy(isSearchingEditor = true)
        editorSearchJob?.cancel()
        editorSearchJob = viewModelScope.launch {
            delay(250)
            try {
                val students = repository.searchAudience(
                    q = query,
                    limit = 50,
                    unassigned = null
                ).items.filterIsInstance<AudienceStudent>()
                _uiState.value = _uiState.value.copy(
                    isSearchingEditor = false,
                    editorResults = students,
                    knownStudents = _uiState.value.knownStudents + students.associateBy { it.id }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isSearchingEditor = false,
                    errorMessage = error.toUiMessage("No fue posible buscar estudiantes.")
                )
            }
        }
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
