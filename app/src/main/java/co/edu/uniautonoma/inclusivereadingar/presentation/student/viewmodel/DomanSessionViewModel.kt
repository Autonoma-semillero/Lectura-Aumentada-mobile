package co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.remote.BackendException
import co.edu.uniautonoma.inclusivereadingar.data.repository.DomanSessionDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.DomanSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.DomanSessionCard
import co.edu.uniautonoma.inclusivereadingar.domain.model.OngoingDomanSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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

    val canRunTimer: Boolean
        get() = currentCard != null && !isPaused && !isCompleted && errorMessage.isNullOrBlank()
}

class DomanSessionViewModel(
    private val domanRepository: DomanSessionDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(DomanSessionUiState())
    val uiState: StateFlow<DomanSessionUiState> = _uiState.asStateFlow()

    private var activeCategoryId: String? = null
    private var activeCategoryName: String? = null
    private var lastShownCardId: String? = null
    private var loadJob: Job? = null
    private var loadGeneration: Long = 0

    fun resumeOrStart(categoryId: String, categoryName: String) {
        val state = _uiState.value
        if (activeCategoryId == categoryId && (state.session != null || loadJob?.isActive == true)) {
            return
        }
        val generation = ++loadGeneration
        loadJob?.cancel()
        activeCategoryId = categoryId
        activeCategoryName = categoryName
        loadJob = viewModelScope.launch {
            _uiState.value = DomanSessionUiState(isLoading = true)
            try {
                val (session, index) = loadStoredOrPrepareSession(categoryId)
                if (generation != loadGeneration) {
                    return@launch
                }
                _uiState.value = DomanSessionUiState(
                    isLoading = false,
                    session = session,
                    currentIndex = index
                )
                persistSnapshot(index)
                registerShownIfNeeded()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (generation != loadGeneration) {
                    return@launch
                }
                _uiState.value = DomanSessionUiState(
                    isLoading = false,
                    errorMessage = error.toUiMessage("No fue posible iniciar la sesión del día.")
                )
            }
        }
    }

    private suspend fun loadStoredOrPrepareSession(categoryId: String): Pair<DomanSession, Int> {
        val snapshot = domanRepository.getOngoingSession()
        if (snapshot == null || snapshot.categoryId != categoryId) {
            return prepareUsableSession(categoryId)
        }

        try {
            val resumed = domanRepository.loadSession(snapshot.sessionId)
            if (resumed.cards.isNotEmpty()) {
                return resumed to snapshot.currentCardIndex.coerceIn(0, resumed.cards.lastIndex)
            }
        } catch (error: BackendException) {
            if (error.statusCode != 404) {
                throw error
            }
        }
        domanRepository.clearOngoingSession()
        return prepareUsableSession(categoryId)
    }

    private suspend fun prepareUsableSession(categoryId: String): Pair<DomanSession, Int> {
        val session = domanRepository.prepareSession(categoryId)
        if (session.cards.isEmpty()) {
            domanRepository.clearOngoingSession()
            throw IllegalStateException(EMPTY_SESSION_MESSAGE)
        }
        return session to 0
    }

    fun retry(categoryId: String, categoryName: String) {
        if (_uiState.value.session != null) {
            _uiState.update { it.copy(errorMessage = null) }
            return
        }
        resumeOrStart(categoryId, categoryName)
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
                handleSessionActionFailure(error, "No fue posible avanzar la sesión.")
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
                handleSessionActionFailure(error, "No fue posible omitir la tarjeta.")
            }
        }
    }

    private suspend fun handleSessionActionFailure(error: Throwable, fallback: String) {
        val message = error.toUiMessage(fallback)
        if (error is BackendException && error.statusCode == 404) {
            runCatching { domanRepository.clearOngoingSession() }
            lastShownCardId = null
            _uiState.value = DomanSessionUiState(
                isLoading = false,
                errorMessage = message
            )
            return
        }
        _uiState.update { it.copy(errorMessage = message) }
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
    private val domanRepository: DomanSessionDataSource
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

private const val EMPTY_SESSION_MESSAGE =
    "La sesión no contiene tarjetas disponibles. Intenta de nuevo."
