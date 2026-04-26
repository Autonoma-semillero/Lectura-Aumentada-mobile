package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.TeacherContentDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.AppUser
import co.edu.uniautonoma.inclusivereadingar.domain.model.Category
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateWordCardUiState(
    val isLoadingStudents: Boolean = true,
    val isLoadingCategories: Boolean = true,
    val isSaving: Boolean = false,
    val students: List<AppUser> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedStudentId: String? = null,
    val word: String = "",
    val selectedCategoryId: String? = null,
    val audioUrl: String = "",
    val errorMessage: String? = null,
    val saved: Boolean = false
) {
    val canSave: Boolean
        get() = selectedStudentId != null && word.isNotBlank() && selectedCategoryId != null
}

class CreateWordCardViewModel(
    private val repository: TeacherContentDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateWordCardUiState())
    val uiState: StateFlow<CreateWordCardUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun selectStudent(id: String) {
        _uiState.update { it.copy(selectedStudentId = id, errorMessage = null) }
    }

    fun updateWord(value: String) {
        _uiState.update { it.copy(word = value.take(15), errorMessage = null) }
    }

    fun selectCategory(id: String) {
        _uiState.update { it.copy(selectedCategoryId = id, errorMessage = null) }
    }

    fun updateAudioUrl(value: String) {
        _uiState.update { it.copy(audioUrl = value, errorMessage = null) }
    }

    fun save() {
        val snapshot = uiState.value
        if (!snapshot.canSave) {
            _uiState.update { it.copy(errorMessage = "Completa estudiante, palabra y temática.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                repository.createWordCard(
                    studentId = requireNotNull(snapshot.selectedStudentId),
                    word = snapshot.word.trim(),
                    categoryId = requireNotNull(snapshot.selectedCategoryId),
                    audioUrl = snapshot.audioUrl.trim().ifBlank { null }
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saved = true) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.toUiMessage("No fue posible guardar la tarjeta.")
                    )
                }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            runCatching {
                awaitAll(
                    async { repository.getStudents() },
                    async { repository.getCategories() }
                )
            }.onSuccess { results ->
                @Suppress("UNCHECKED_CAST")
                val students = results[0] as List<AppUser>
                @Suppress("UNCHECKED_CAST")
                val categories = results[1] as List<Category>
                _uiState.update {
                    it.copy(
                        isLoadingStudents = false,
                        isLoadingCategories = false,
                        students = students,
                        categories = categories.sortedBy { category -> category.sortOrder }
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingStudents = false,
                        isLoadingCategories = false,
                        errorMessage = error.toUiMessage("No fue posible cargar la información inicial.")
                    )
                }
            }
        }
    }
}

class CreateWordCardViewModelFactory(
    private val repository: TeacherContentDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateWordCardViewModel::class.java)) {
            return CreateWordCardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
