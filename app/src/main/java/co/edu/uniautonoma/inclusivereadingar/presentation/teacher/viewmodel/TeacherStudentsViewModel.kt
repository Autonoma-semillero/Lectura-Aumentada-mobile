package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.TeacherContentDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.AppUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeacherStudentsUiState(
    val isLoading: Boolean = true,
    val students: List<AppUser> = emptyList(),
    val errorMessage: String? = null
)

class TeacherStudentsViewModel(
    private val repository: TeacherContentDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeacherStudentsUiState())
    val uiState: StateFlow<TeacherStudentsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            runCatching {
                repository.getStudents()
            }.onSuccess { students ->
                _uiState.value = TeacherStudentsUiState(
                    isLoading = false,
                    students = students
                )
            }.onFailure { error ->
                _uiState.value = TeacherStudentsUiState(
                    isLoading = false,
                    errorMessage = error.toUiMessage("No fue posible cargar los estudiantes.")
                )
            }
        }
    }
}

class TeacherStudentsViewModelFactory(
    private val repository: TeacherContentDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherStudentsViewModel::class.java)) {
            return TeacherStudentsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
