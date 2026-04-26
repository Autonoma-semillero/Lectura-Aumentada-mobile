package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.DomanRepository
import co.edu.uniautonoma.inclusivereadingar.domain.model.DailyPlanSummary
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentProgressSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeacherDomanPlansUiState(
    val isLoading: Boolean = true,
    val plan: DailyPlanSummary? = null,
    val summary: StudentProgressSummary? = null,
    val errorMessage: String? = null,
    val isGenerating: Boolean = false
)

class TeacherDomanPlansViewModel(
    private val repository: DomanRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeacherDomanPlansUiState())
    val uiState: StateFlow<TeacherDomanPlansUiState> = _uiState.asStateFlow()

    fun load(studentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                repository.getTodayPlan(studentId) to repository.getStudentProgressSummary(studentId)
            }.onSuccess { (plan, summary) ->
                _uiState.value = TeacherDomanPlansUiState(
                    isLoading = false,
                    plan = plan,
                    summary = summary
                )
            }.onFailure { error ->
                _uiState.value = TeacherDomanPlansUiState(
                    isLoading = false,
                    errorMessage = error.toUiMessage("No fue posible cargar el plan Doman.")
                )
            }
        }
    }

    fun regenerate(studentId: String, categoryId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, errorMessage = null)
            runCatching {
                repository.generatePlan(studentId, categoryId, true)
            }.onSuccess {
                load(studentId)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = error.toUiMessage("No fue posible regenerar el plan Doman.")
                )
            }
        }
    }
}

class TeacherDomanPlansViewModelFactory(
    private val repository: DomanRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherDomanPlansViewModel::class.java)) {
            return TeacherDomanPlansViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
