package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.DocenteProgressDataSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StudentWithProgress(
    val id: String,
    val email: String,
    val displayName: String?,
    val completedWords: Int,
    val totalActiveWords: Int,
    val progressPercent: Int,
    val hasPhase2Ready: Boolean
) {
    val nameOrEmail: String get() = displayName?.takeIf { it.isNotBlank() } ?: email

    fun initials(): String {
        val name = displayName?.trim()
        if (name.isNullOrBlank()) return email.take(2).uppercase()
        val parts = name.split(" ").filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0][0]}${parts[1][0]}".uppercase()
            else -> parts[0].take(2).uppercase()
        }
    }
}

data class TeacherStudentsUiState(
    val isLoading: Boolean = true,
    val students: List<StudentWithProgress> = emptyList(),
    val errorMessage: String? = null
)

class TeacherStudentsViewModel(
    private val repository: DocenteProgressDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeacherStudentsUiState())
    val uiState: StateFlow<TeacherStudentsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = TeacherStudentsUiState(isLoading = true)
            runCatching {
                val students = repository.getStudents()
                coroutineScope {
                    students.map { student ->
                        async {
                            val progress = runCatching {
                                repository.getStudentCategoryProgress(student.id)
                            }.getOrDefault(emptyList())

                            val completedWords = progress.sumOf { it.byStatus.completed }
                            val totalActiveWords = progress.sumOf { it.activeWords }
                            val progressPercent = if (totalActiveWords > 0)
                                completedWords * 100 / totalActiveWords
                            else 0
                            val hasPhase2Ready = progress.any { it.phase2Ready }

                            StudentWithProgress(
                                id = student.id,
                                email = student.email,
                                displayName = student.displayName,
                                completedWords = completedWords,
                                totalActiveWords = totalActiveWords,
                                progressPercent = progressPercent,
                                hasPhase2Ready = hasPhase2Ready
                            )
                        }
                    }.awaitAll()
                }
            }.onSuccess { students ->
                _uiState.value = TeacherStudentsUiState(isLoading = false, students = students)
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
    private val repository: DocenteProgressDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherStudentsViewModel::class.java)) {
            return TeacherStudentsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
