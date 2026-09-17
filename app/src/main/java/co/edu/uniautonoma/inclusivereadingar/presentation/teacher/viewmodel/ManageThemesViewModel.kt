package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.remote.BackendException
import co.edu.uniautonoma.inclusivereadingar.data.repository.TeacherContentDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManageThemesUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val errorMessage: String? = null,
    val deleteConfirmId: String? = null
)

class ManageThemesViewModel(
    private val repository: TeacherContentDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(ManageThemesUiState())
    val uiState: StateFlow<ManageThemesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.getCategories()
            }.onSuccess { categories ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        categories = categories.sortedBy { category -> category.sortOrder },
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.toUiMessage("No fue posible cargar las temáticas.")
                    )
                }
            }
        }
    }

    fun requestDelete(id: String) {
        _uiState.update { it.copy(deleteConfirmId = id) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(deleteConfirmId = null) }
    }

    fun confirmDelete() {
        val categoryId = _uiState.value.deleteConfirmId ?: return
        viewModelScope.launch {
            runCatching {
                repository.deleteCategory(categoryId)
            }.onSuccess {
                _uiState.update { it.copy(deleteConfirmId = null) }
                load()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        deleteConfirmId = null,
                        errorMessage = error.toUiMessage("No fue posible eliminar la temática.")
                    )
                }
            }
        }
    }
}

class ManageThemesViewModelFactory(
    private val repository: TeacherContentDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageThemesViewModel::class.java)) {
            return ManageThemesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

internal fun Throwable.toUiMessage(fallback: String): String = when (this) {
    is BackendException -> message
    else -> message?.takeIf { it.isNotBlank() } ?: fallback
}
