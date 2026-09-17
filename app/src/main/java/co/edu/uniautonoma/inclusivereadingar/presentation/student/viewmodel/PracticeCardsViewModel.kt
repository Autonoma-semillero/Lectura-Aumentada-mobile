package co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.remote.BackendException
import co.edu.uniautonoma.inclusivereadingar.data.repository.StudentContentDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.WordCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PracticeCardsUiState(
    val isLoading: Boolean = true,
    val cards: List<WordCard> = emptyList(),
    val currentIndex: Int = 0,
    val errorMessage: String? = null,
    val completedCount: Int = 0
) {
    val currentCard: WordCard?
        get() = cards.getOrNull(currentIndex)
}

class PracticeCardsViewModel(
    private val studentContentRepository: StudentContentDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(PracticeCardsUiState())
    val uiState: StateFlow<PracticeCardsUiState> = _uiState.asStateFlow()

    private var currentCategoryId: String? = null
    private var currentCategoryName: String? = null

    fun load(categoryId: String, categoryName: String) {
        if (
            !uiState.value.isLoading &&
            currentCategoryId == categoryId &&
            currentCategoryName == categoryName &&
            uiState.value.cards.isNotEmpty()
        ) {
            return
        }

        currentCategoryId = categoryId
        currentCategoryName = categoryName

        viewModelScope.launch {
            _uiState.value = PracticeCardsUiState(isLoading = true)
            runCatching {
                studentContentRepository.getWordCards(categoryId)
            }.onSuccess { cards ->
                _uiState.value = PracticeCardsUiState(
                    isLoading = false,
                    cards = cards
                )
                registerViewedCurrentCard()
            }.onFailure { error ->
                val message = when (error) {
                    is BackendException -> error.message
                    else -> "No fue posible cargar las palabras."
                }
                _uiState.value = PracticeCardsUiState(
                    isLoading = false,
                    errorMessage = message
                )
            }
        }
    }

    fun completeCurrentWord() {
        val currentCard = _uiState.value.currentCard ?: return
        val categoryName = currentCategoryName ?: return

        viewModelScope.launch {
            runCatching {
                studentContentRepository.registerWordCompleted(currentCard, categoryName)
            }.onSuccess {
                _uiState.update { state ->
                    val nextIndex = (state.currentIndex + 1).coerceAtMost(state.cards.lastIndex)
                    state.copy(
                        currentIndex = nextIndex,
                        completedCount = state.completedCount + 1,
                        errorMessage = null
                    )
                }
                registerViewedCurrentCard()
            }.onFailure { error ->
                val message = when (error) {
                    is BackendException -> error.message
                    else -> "No fue posible registrar el progreso."
                }
                _uiState.update { it.copy(errorMessage = message) }
            }
        }
    }

    fun moveToCard(index: Int) {
        val state = _uiState.value
        if (index < 0 || index > state.cards.lastIndex || index == state.currentIndex) return
        _uiState.update { it.copy(currentIndex = index, errorMessage = null) }
        registerViewedCurrentCard()
    }

    fun moveToNextCard() {
        val state = _uiState.value
        if (state.currentIndex >= state.cards.lastIndex) return
        _uiState.update { it.copy(currentIndex = it.currentIndex + 1, errorMessage = null) }
        registerViewedCurrentCard()
    }

    fun moveToPreviousCard() {
        _uiState.update { state ->
            state.copy(currentIndex = (state.currentIndex - 1).coerceAtLeast(0), errorMessage = null)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun registerViewedCurrentCard() {
        val currentCard = _uiState.value.currentCard ?: return
        val categoryName = currentCategoryName ?: return

        viewModelScope.launch {
            runCatching {
                studentContentRepository.registerWordViewed(currentCard, categoryName)
            }
        }
    }
}

class PracticeCardsViewModelFactory(
    private val studentContentRepository: StudentContentDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PracticeCardsViewModel::class.java)) {
            return PracticeCardsViewModel(studentContentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
