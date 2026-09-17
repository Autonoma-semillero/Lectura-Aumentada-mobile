package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.remote.BackendException
import co.edu.uniautonoma.inclusivereadingar.data.repository.DomanRepository
import co.edu.uniautonoma.inclusivereadingar.data.repository.TeacherContentDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.Category
import co.edu.uniautonoma.inclusivereadingar.domain.model.DailyPlanSummary
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentProgressSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeacherDomanPlansUiState(
    val isLoading: Boolean = true,
    val plans: List<DailyPlanSummary> = emptyList(),
    val summary: StudentProgressSummary? = null,
    val allCategories: List<Category> = emptyList(),
    val dialogCategories: List<Category> = emptyList(),
    val errorMessage: String? = null,
    val showGenerateDialog: Boolean = false,
    val isGenerating: Boolean = false,
    val regeneratingPlanId: String? = null,
    val deletingPlanId: String? = null,
    val deleteConfirmPlanId: String? = null
)

class TeacherDomanPlansViewModel(
    private val domanRepository: DomanRepository,
    private val contentRepository: TeacherContentDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeacherDomanPlansUiState())
    val uiState: StateFlow<TeacherDomanPlansUiState> = _uiState.asStateFlow()

    fun load(studentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val plansDeferred = async { domanRepository.getTodayPlans(studentId) }
                val summaryDeferred = async {
                    runCatching { domanRepository.getStudentProgressSummary(studentId) }.getOrNull()
                }
                val categoriesDeferred = async { contentRepository.getCategories() }
                Triple(plansDeferred.await(), summaryDeferred.await(), categoriesDeferred.await())
            }.onSuccess { (plans, summary, categories) ->
                val planCategoryIds = plans.map { it.categoryId }.toSet()
                val dialogCategories = categories.filter { it.id !in planCategoryIds }
                _uiState.value = TeacherDomanPlansUiState(
                    isLoading = false,
                    plans = plans,
                    summary = summary,
                    allCategories = categories,
                    dialogCategories = dialogCategories
                )
            }.onFailure { error ->
                _uiState.value = TeacherDomanPlansUiState(
                    isLoading = false,
                    errorMessage = error.toUiMessage("No fue posible cargar los planes Doman.")
                )
            }
        }
    }

    fun generatePlan(studentId: String, categoryId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, showGenerateDialog = false, errorMessage = null)
            runCatching {
                domanRepository.generatePlan(studentId, categoryId, false)
            }.onSuccess {
                load(studentId)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = error.toUiMessage("No fue posible generar el plan.")
                )
            }
        }
    }

    fun regeneratePlan(studentId: String, planId: String, categoryId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(regeneratingPlanId = planId, errorMessage = null)
            runCatching {
                domanRepository.generatePlan(studentId, categoryId, true)
            }.onSuccess {
                load(studentId)
            }.onFailure { error ->
                val message = if (error is BackendException && error.statusCode == 409) {
                    "Este plan ya tiene sesiones completadas y no puede regenerarse."
                } else {
                    error.toUiMessage("No fue posible regenerar el plan.")
                }
                _uiState.value = _uiState.value.copy(regeneratingPlanId = null, errorMessage = message)
            }
        }
    }

    fun requestDeletePlan(planId: String) {
        _uiState.value = _uiState.value.copy(deleteConfirmPlanId = planId)
    }

    fun cancelDeletePlan() {
        _uiState.value = _uiState.value.copy(deleteConfirmPlanId = null)
    }

    fun confirmDeletePlan(studentId: String) {
        val planId = _uiState.value.deleteConfirmPlanId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deletingPlanId = planId, deleteConfirmPlanId = null, errorMessage = null)
            runCatching {
                domanRepository.deletePlan(planId)
            }.onSuccess {
                load(studentId)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    deletingPlanId = null,
                    errorMessage = error.toUiMessage("No fue posible eliminar el plan.")
                )
            }
        }
    }

    fun openGenerateDialog() {
        _uiState.value = _uiState.value.copy(showGenerateDialog = true)
    }

    fun dismissGenerateDialog() {
        _uiState.value = _uiState.value.copy(showGenerateDialog = false)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

class TeacherDomanPlansViewModelFactory(
    private val domanRepository: DomanRepository,
    private val contentRepository: TeacherContentDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherDomanPlansViewModel::class.java)) {
            return TeacherDomanPlansViewModel(domanRepository, contentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
