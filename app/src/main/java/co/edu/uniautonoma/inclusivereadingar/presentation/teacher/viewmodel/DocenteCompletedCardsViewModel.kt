package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.DocenteProgressDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.CompletedCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DocenteCompletedCardsUiState(
    val isLoadingInitial: Boolean = true,
    val isLoadingMore: Boolean = false,
    val cards: List<CompletedCard> = emptyList(),
    val totalCards: Int = 0,
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val errorMessage: String? = null,
    val loadMoreError: String? = null
)

class DocenteCompletedCardsViewModel(
    private val repository: DocenteProgressDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(DocenteCompletedCardsUiState())
    val uiState: StateFlow<DocenteCompletedCardsUiState> = _uiState.asStateFlow()

    private var studentId = ""
    private var categoryId = ""
    private var initialized = false

    fun init(studentId: String, categoryId: String) {
        if (initialized) return
        initialized = true
        this.studentId = studentId
        this.categoryId = categoryId
        loadInitial()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _uiState.value = DocenteCompletedCardsUiState(isLoadingInitial = true)
            runCatching {
                repository.getStudentCompletedCards(studentId, categoryId, 1, PAGE_SIZE)
            }.onSuccess { page ->
                _uiState.value = DocenteCompletedCardsUiState(
                    isLoadingInitial = false,
                    cards = page.data,
                    totalCards = page.total,
                    currentPage = 1,
                    hasMore = page.hasMore
                )
            }.onFailure { error ->
                _uiState.value = DocenteCompletedCardsUiState(
                    isLoadingInitial = false,
                    errorMessage = error.toUiMessage("No fue posible cargar las palabras dominadas.")
                )
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, loadMoreError = null) }
            val nextPage = state.currentPage + 1
            runCatching {
                repository.getStudentCompletedCards(studentId, categoryId, nextPage, PAGE_SIZE)
            }.onSuccess { page ->
                _uiState.update { current ->
                    current.copy(
                        isLoadingMore = false,
                        cards = current.cards + page.data,
                        totalCards = page.total,
                        currentPage = nextPage,
                        hasMore = page.hasMore
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        loadMoreError = error.toUiMessage("Error al cargar más palabras.")
                    )
                }
            }
        }
    }

    fun retry() {
        initialized = false
        init(studentId, categoryId)
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}

class DocenteCompletedCardsViewModelFactory(
    private val repository: DocenteProgressDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DocenteCompletedCardsViewModel::class.java)) {
            return DocenteCompletedCardsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
