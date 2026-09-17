package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.TeacherContentDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WordCardCategoryListUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val errorMessage: String? = null
)

class WordCardCategoryListViewModel(
    private val repository: TeacherContentDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(WordCardCategoryListUiState())
    val uiState: StateFlow<WordCardCategoryListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.getCategories() }
                .onSuccess { categories ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            categories = categories.sortedBy { c -> c.sortOrder }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No fue posible cargar las temáticas."
                        )
                    }
                }
        }
    }
}

class WordCardCategoryListViewModelFactory(
    private val repository: TeacherContentDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WordCardCategoryListViewModel::class.java)) {
            return WordCardCategoryListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
