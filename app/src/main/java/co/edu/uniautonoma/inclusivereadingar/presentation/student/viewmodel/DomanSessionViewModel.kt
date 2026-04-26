package co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.remote.BackendException
import co.edu.uniautonoma.inclusivereadingar.data.repository.DomanRepository
import co.edu.uniautonoma.inclusivereadingar.domain.model.DomanSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.DomanSessionCard
import co.edu.uniautonoma.inclusivereadingar.domain.model.OngoingDomanSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DomanSessionUiState(
    val isLoading: Boolean = true,
    val session: DomanSession? = null,
    val currentIndex: Int = 0,
    val isPaused: Boolean = false,
    val errorMessage: String? = null,
    val isCompleted: Boolean = false
) {
    val currentCard: DomanSessionCard?
        get() = session?.cards?.getOrNull(currentIndex)
}

class DomanSessionViewModel(
    private val domanRepository: DomanRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DomanSessionUiState())
    val uiState: StateFlow<DomanSessionUiState> = _uiState.asStateFlow()

    private var activeCategoryId: String? = null
    private var activeCategoryName: String? = null
    private var lastShownCardId: String? = null

    fun resumeOrStart(categoryId: String, categoryName: String) {
        if (_uiState.value.session != null && activeCategoryId == categoryId) {
            return
        }
        activeCategoryId = categoryId
        activeCategoryName = categoryName
        viewModelScope.launch {
            _uiState.value = DomanSessionUiState(isLoading = true)
            runCatching {
                val snapshot = domanRepository.getOngoingSession()
                if (snapshot != null && snapshot.categoryId == categoryId) {
                    val resumed = domanRepository.loadSession(snapshot.sessionId)
                    resumed to snapshot.currentCardIndex.coerceIn(0, resumed.cards.lastIndex.coerceAtLeast(0))
                } else {
                    domanRepository.prepareSession(categoryId) to 0
                }
            }.onSuccess { (session, index) ->
                _uiState.value = DomanSessionUiState(
                    isLoading = false,
                    session = session,
                    currentIndex = index
                )
                persistSnapshot(index)
                registerShownIfNeeded()
            }.onFailure { error ->
                _uiState.value = DomanSessionUiState(
                    isLoading = false,
                    errorMessage = error.toUiMessage("No fue posible iniciar la sesión del día.")
                )
            }
        }
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun advance() {
        val state = _uiState.value
        val session = state.session ?: return
        val currentCard = state.currentCard ?: return
        viewModelScope.launch {
            runCatching {
                domanRepository.registerCardCompleted(session.sessionId, currentCard.id)
                if (state.currentIndex >= session.cards.lastIndex) {
                    domanRepository.completeSession(session.sessionId)
                    domanRepository.clearOngoingSession()
                    _uiState.update { it.copy(isCompleted = true, isPaused = true, errorMessage = null) }
                } else {
                    val nextIndex = state.currentIndex + 1
                    lastShownCardId = null
                    _uiState.update { it.copy(currentIndex = nextIndex, errorMessage = null) }
                    persistSnapshot(nextIndex)
                    registerShownIfNeeded()
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.toUiMessage("No fue posible avanzar la sesión."))
                }
            }
        }
    }

    fun skip() {
        val state = _uiState.value
        val session = state.session ?: return
        val currentCard = state.currentCard ?: return
        viewModelScope.launch {
            runCatching {
                domanRepository.registerCardSkipped(session.sessionId, currentCard.id)
                if (state.currentIndex >= session.cards.lastIndex) {
                    domanRepository.completeSession(session.sessionId)
                    domanRepository.clearOngoingSession()
                    _uiState.update { it.copy(isCompleted = true, isPaused = true, errorMessage = null) }
                } else {
                    val nextIndex = state.currentIndex + 1
                    lastShownCardId = null
                    _uiState.update { it.copy(currentIndex = nextIndex, errorMessage = null) }
                    persistSnapshot(nextIndex)
                    registerShownIfNeeded()
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.toUiMessage("No fue posible omitir la tarjeta."))
                }
            }
        }
    }

    fun registerAudioPlayed() {
        val state = _uiState.value
        val session = state.session ?: return
        val currentCard = state.currentCard ?: return
        viewModelScope.launch {
            runCatching {
                domanRepository.registerAudioPlayed(session.sessionId, currentCard.id)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun registerShownIfNeeded() {
        val state = _uiState.value
        val session = state.session ?: return
        val currentCard = state.currentCard ?: return
        if (currentCard.id == lastShownCardId) {
            return
        }
        lastShownCardId = currentCard.id
        viewModelScope.launch {
            runCatching {
                domanRepository.registerCardShown(
                    sessionId = session.sessionId,
                    wordCardId = currentCard.id,
                    displayMs = session.displayMs
                )
            }
        }
    }

    private fun persistSnapshot(currentIndex: Int) {
        val session = _uiState.value.session ?: return
        val categoryId = activeCategoryId ?: return
        val categoryName = activeCategoryName ?: return
        viewModelScope.launch {
            domanRepository.saveOngoingSession(
                OngoingDomanSession(
                    sessionId = session.sessionId,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    currentCardIndex = currentIndex
                )
            )
        }
    }
}

class DomanSessionViewModelFactory(
    private val domanRepository: DomanRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DomanSessionViewModel::class.java)) {
            return DomanSessionViewModel(domanRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private fun Throwable.toUiMessage(fallback: String): String = when (this) {
    is BackendException -> message
    else -> message?.takeIf { it.isNotBlank() } ?: fallback
}
