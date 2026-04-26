package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.DomanRepository
import co.edu.uniautonoma.inclusivereadingar.domain.model.DomanSessionHistoryItem
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentProgressSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeacherStudentProgressUiState(
    val isLoading: Boolean = true,
    val summary: StudentProgressSummary? = null,
    val history: List<DomanSessionHistoryItem> = emptyList(),
    val errorMessage: String? = null
)

class TeacherStudentProgressViewModel(
    private val repository: DomanRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeacherStudentProgressUiState())
    val uiState: StateFlow<TeacherStudentProgressUiState> = _uiState.asStateFlow()

    fun load(studentId: String) {
        viewModelScope.launch {
            runCatching {
                repository.getStudentProgressSummary(studentId) to repository.getStudentsHistory(studentId)
            }.onSuccess { (summary, history) ->
                _uiState.value = TeacherStudentProgressUiState(
                    isLoading = false,
                    summary = summary,
                    history = history
                )
            }.onFailure { error ->
                _uiState.value = TeacherStudentProgressUiState(
                    isLoading = false,
                    errorMessage = error.toUiMessage("No fue posible cargar el progreso del estudiante.")
                )
            }
        }
    }
}

class TeacherStudentProgressViewModelFactory(
    private val repository: DomanRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherStudentProgressViewModel::class.java)) {
            return TeacherStudentProgressViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
