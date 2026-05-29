package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.DocenteProgressDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentCategoryProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeacherStudentProgressUiState(
    val isLoading: Boolean = true,
    val categories: List<StudentCategoryProgress> = emptyList(),
    val errorMessage: String? = null
)

class TeacherStudentProgressViewModel(
    private val repository: DocenteProgressDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeacherStudentProgressUiState())
    val uiState: StateFlow<TeacherStudentProgressUiState> = _uiState.asStateFlow()

    fun load(studentId: String) {
        viewModelScope.launch {
            _uiState.value = TeacherStudentProgressUiState(isLoading = true)
            runCatching {
                repository.getStudentCategoryProgress(studentId)
            }.onSuccess { categories ->
                _uiState.value = TeacherStudentProgressUiState(
                    isLoading = false,
                    categories = categories
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
    private val repository: DocenteProgressDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherStudentProgressViewModel::class.java)) {
            return TeacherStudentProgressViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
