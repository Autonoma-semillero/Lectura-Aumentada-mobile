package co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.remote.BackendException
import co.edu.uniautonoma.inclusivereadingar.data.repository.StudentContentDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.CategorySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ThemesUiState(
    val isLoading: Boolean = true,
    val categories: List<CategorySummary> = emptyList(),
    val errorMessage: String? = null
)

class ThemesViewModel(
    private val studentContentRepository: StudentContentDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(ThemesUiState())
    val uiState: StateFlow<ThemesUiState> = _uiState.asStateFlow()

    init {
        loadThemes()
    }

    fun loadThemes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                studentContentRepository.getAvailableCategories()
            }.onSuccess { categories ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        categories = categories,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                val message = when (error) {
                    is BackendException -> error.message
                    else -> "No fue posible cargar las temáticas."
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                }
            }
        }
    }
}

class ThemesViewModelFactory(
    private val studentContentRepository: StudentContentDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemesViewModel::class.java)) {
            return ThemesViewModel(studentContentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
