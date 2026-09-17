package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.DomanRepository
import co.edu.uniautonoma.inclusivereadingar.data.repository.GroupsDataSource
import co.edu.uniautonoma.inclusivereadingar.data.repository.TeacherContentDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceGroup
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceSearchItem
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceStudent
import co.edu.uniautonoma.inclusivereadingar.domain.model.BulkPlanGenerationResult
import co.edu.uniautonoma.inclusivereadingar.domain.model.Category
import co.edu.uniautonoma.inclusivereadingar.domain.model.PlanAudienceSelection
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeacherPlanAssignmentUiState(
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val isGenerating: Boolean = false,
    val query: String = "",
    val searchItems: List<AudienceSearchItem> = emptyList(),
    val knownGroups: Map<String, AudienceGroup> = emptyMap(),
    val knownStudents: Map<String, AudienceStudent> = emptyMap(),
    val selection: PlanAudienceSelection = PlanAudienceSelection(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val result: BulkPlanGenerationResult? = null,
    val errorMessage: String? = null
) {
    val resolvedStudentIds: Set<String>
        get() = selection.resolvedStudentIds(knownGroups.values)

    val canGenerate: Boolean
        get() = !isGenerating && selectedCategoryId != null && resolvedStudentIds.isNotEmpty()
}

class TeacherPlanAssignmentViewModel(
    private val groupsRepository: GroupsDataSource,
    private val contentRepository: TeacherContentDataSource,
    private val domanRepository: DomanRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeacherPlanAssignmentUiState())
    val uiState: StateFlow<TeacherPlanAssignmentUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null
    private var loaded = false

    fun load(preselectedStudentId: String? = null, preselectedStudentName: String? = null) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val categories = async { contentRepository.getCategories() }
                val audience = async { groupsRepository.searchAudience("", 100) }
                categories.await() to audience.await().items
            }.onSuccess { (categories, items) ->
                val groups = items.filterIsInstance<AudienceGroup>().associateBy { it.id }
                val students = items.filterIsInstance<AudienceStudent>().associateBy { it.id }.toMutableMap()
                val selectedIds = preselectedStudentId?.takeIf { it.isNotBlank() }?.let(::setOf).orEmpty()
                if (!preselectedStudentId.isNullOrBlank() && preselectedStudentId !in students) {
                    students[preselectedStudentId] = AudienceStudent(
                        audienceKey = "student:$preselectedStudentId",
                        id = preselectedStudentId,
                        displayName = preselectedStudentName,
                        email = preselectedStudentName.orEmpty(),
                        username = null,
                        groupIds = emptyList(),
                        unassigned = true
                    )
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    searchItems = items,
                    knownGroups = groups,
                    knownStudents = students,
                    categories = categories,
                    selectedCategoryId = categories.firstOrNull()?.id,
                    selection = PlanAudienceSelection(studentIds = selectedIds)
                )
            }.onFailure { error ->
                loaded = false
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.toUiMessage("No fue posible preparar la asignación.")
                )
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query, isSearching = true)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250)
            try {
                mergeSearchItems(groupsRepository.searchAudience(query, 50).items)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    errorMessage = error.toUiMessage("No fue posible buscar estudiantes y grupos.")
                )
            }
        }
    }

    fun toggle(item: AudienceSearchItem) {
        val state = _uiState.value
        val newSelection = when (item) {
            is AudienceGroup -> state.selection.copy(
                groupIds = state.selection.groupIds.toggle(item.id)
            )

            is AudienceStudent -> state.selection.copy(
                studentIds = state.selection.studentIds.toggle(item.id)
            )
        }
        _uiState.value = state.copy(
            selection = newSelection,
            knownGroups = if (item is AudienceGroup) state.knownGroups + (item.id to item) else state.knownGroups,
            knownStudents = if (item is AudienceStudent) state.knownStudents + (item.id to item) else state.knownStudents,
            result = null
        )
    }

    fun removeGroup(groupId: String) {
        _uiState.value = _uiState.value.copy(
            selection = _uiState.value.selection.copy(
                groupIds = _uiState.value.selection.groupIds - groupId
            ),
            result = null
        )
    }

    fun removeStudent(studentId: String) {
        _uiState.value = _uiState.value.copy(
            selection = _uiState.value.selection.copy(
                studentIds = _uiState.value.selection.studentIds - studentId
            ),
            result = null
        )
    }

    fun selectCategory(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId, result = null)
    }

    fun generate() {
        val state = _uiState.value
        val categoryId = state.selectedCategoryId
        if (state.resolvedStudentIds.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Selecciona al menos un estudiante o grupo con estudiantes.")
            return
        }
        if (categoryId == null) {
            _uiState.value = state.copy(errorMessage = "Selecciona una categoría.")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isGenerating = true, errorMessage = null, result = null)
            runCatching {
                domanRepository.generateBulkPlan(
                    groupIds = state.selection.groupIds,
                    studentIds = state.selection.studentIds,
                    categoryId = categoryId,
                    force = false
                )
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(isGenerating = false, result = result)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = error.toUiMessage("No fue posible asignar el plan.")
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun retry() {
        loaded = false
        load()
    }

    private fun mergeSearchItems(items: List<AudienceSearchItem>) {
        val state = _uiState.value
        _uiState.value = state.copy(
            isSearching = false,
            searchItems = items,
            knownGroups = state.knownGroups + items.filterIsInstance<AudienceGroup>().associateBy { it.id },
            knownStudents = state.knownStudents + items.filterIsInstance<AudienceStudent>().associateBy { it.id }
        )
    }

    private fun Set<String>.toggle(value: String): Set<String> =
        if (value in this) this - value else this + value
}

class TeacherPlanAssignmentViewModelFactory(
    private val groupsRepository: GroupsDataSource,
    private val contentRepository: TeacherContentDataSource,
    private val domanRepository: DomanRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherPlanAssignmentViewModel::class.java)) {
            return TeacherPlanAssignmentViewModel(
                groupsRepository = groupsRepository,
                contentRepository = contentRepository,
                domanRepository = domanRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
