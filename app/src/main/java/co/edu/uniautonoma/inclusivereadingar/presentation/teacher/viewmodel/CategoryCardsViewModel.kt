package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.TeacherContentDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.AppUser
import co.edu.uniautonoma.inclusivereadingar.domain.model.WordCard
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryCardsUiState(
    val isLoading: Boolean = true,
    val cards: List<WordCard> = emptyList(),
    val students: List<AppUser> = emptyList(),
    val errorMessage: String? = null,
    val archiveConfirmCardId: String? = null,
    val isArchiving: Boolean = false
)

class CategoryCardsViewModel(
    private val repository: TeacherContentDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoryCardsUiState())
    val uiState: StateFlow<CategoryCardsUiState> = _uiState.asStateFlow()

    private var currentCategoryId: String? = null

    fun load(categoryId: String) {
        currentCategoryId = categoryId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                awaitAll(
                    async { repository.getWordCardsForCategory(categoryId) },
                    async { repository.getStudents() }
                )
            }.onSuccess { results ->
                @Suppress("UNCHECKED_CAST")
                val cards = (results[0] as List<WordCard>)
                    .filter { it.status != "archived" }
                    .sortedBy { it.word }
                @Suppress("UNCHECKED_CAST")
                val students = results[1] as List<AppUser>
                _uiState.update {
                    it.copy(isLoading = false, cards = cards, students = students)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No fue posible cargar las tarjetas."
                    )
                }
            }
        }
    }

    fun requestArchive(cardId: String) {
        _uiState.update { it.copy(archiveConfirmCardId = cardId) }
    }

    fun cancelArchive() {
        _uiState.update { it.copy(archiveConfirmCardId = null) }
    }

    fun confirmArchive() {
        val cardId = _uiState.value.archiveConfirmCardId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isArchiving = true, archiveConfirmCardId = null) }
            runCatching { repository.archiveWordCard(cardId) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isArchiving = false,
                            cards = state.cards.filter { it.id != cardId }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isArchiving = false,
                            errorMessage = error.message ?: "No fue posible eliminar la tarjeta."
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

class CategoryCardsViewModelFactory(
    private val repository: TeacherContentDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoryCardsViewModel::class.java)) {
            return CategoryCardsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
