package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.GroupsDataSource
import co.edu.uniautonoma.inclusivereadingar.data.repository.LiveDomanDataSource
import co.edu.uniautonoma.inclusivereadingar.data.repository.TeacherContentDataSource
import co.edu.uniautonoma.inclusivereadingar.data.remote.LiveDomanRealtimeSubscription
import co.edu.uniautonoma.inclusivereadingar.domain.model.Category
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanCard
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanRepertoire
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSessionDetail
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentGroup
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeacherLiveSessionUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val groups: List<StudentGroup> = emptyList(),
    val repertoires: List<LiveDomanRepertoire> = emptyList(),
    val categories: List<Category> = emptyList(),
    val cards: List<LiveDomanCard> = emptyList(),
    val selectedContentCategoryId: String? = null,
    val selectedCardIds: List<String> = emptyList(),
    val selectedGroupId: String? = null,
    val selectedRepertoireId: String? = null,
    val detail: LiveDomanSessionDetail? = null,
    val errorMessage: String? = null,
    val isRealtimeConnected: Boolean = false
) {
    val session: LiveDomanSession? get() = detail?.session
}

class TeacherLiveSessionViewModel(
    private val groups: GroupsDataSource,
    private val liveDoman: LiveDomanDataSource,
    private val content: TeacherContentDataSource
) : ViewModel() {
    private var subscription: LiveDomanRealtimeSubscription? = null
    private var subscribedSessionId: String? = null
    private val _uiState = MutableStateFlow(TeacherLiveSessionUiState())
    val uiState: StateFlow<TeacherLiveSessionUiState> = _uiState.asStateFlow()

    init { loadSetup() }

    fun loadSetup() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val loadedGroups = groups.getGroups()
            val loadedRepertoires = liveDoman.getRepertoires()
            val loadedCards = liveDoman.getCards()
            val loadedCategories = content.getCategories()
            _uiState.update { state ->
                val selectedCategoryId = state.selectedContentCategoryId
                    ?.takeIf { selected -> loadedCategories.any { it.id == selected } }
                    ?: loadedCategories.firstOrNull()?.id
                state.copy(
                    isLoading = false,
                    groups = loadedGroups,
                    repertoires = loadedRepertoires,
                    categories = loadedCategories,
                    cards = loadedCards,
                    selectedGroupId = state.selectedGroupId ?: loadedGroups.firstOrNull()?.id,
                    selectedRepertoireId = state.selectedRepertoireId ?: loadedRepertoires.firstOrNull()?.id,
                    selectedContentCategoryId = selectedCategoryId,
                    selectedCardIds = state.selectedCardIds.filter { selected -> loadedCards.any { it.id == selected } }
                )
            }
        } catch (error: Throwable) {
            _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
        }
    }

    fun selectGroup(id: String) = _uiState.update { it.copy(selectedGroupId = id) }
    fun selectRepertoire(id: String) = _uiState.update { it.copy(selectedRepertoireId = id) }
    fun selectContentCategory(id: String) = _uiState.update {
        it.copy(selectedContentCategoryId = id, selectedCardIds = emptyList())
    }

    fun toggleCard(id: String) = _uiState.update { state ->
        val selected = state.selectedCardIds
        state.copy(selectedCardIds = if (id in selected) selected - id else selected + id)
    }

    fun moveCard(id: String, delta: Int) = _uiState.update { state ->
        val index = state.selectedCardIds.indexOf(id)
        val destination = index + delta
        if (index < 0 || destination !in state.selectedCardIds.indices) state else {
            val reordered = state.selectedCardIds.toMutableList()
            val card = reordered.removeAt(index)
            reordered.add(destination, card)
            state.copy(selectedCardIds = reordered)
        }
    }

    fun createCard(word: String) {
        val categoryId = _uiState.value.selectedContentCategoryId ?: return
        val normalizedWord = word.trim()
        if (normalizedWord.isBlank()) return
        createContent { liveDoman.createCard(normalizedWord, categoryId) }
    }

    fun createRepertoire(name: String) {
        val state = _uiState.value
        val categoryId = state.selectedContentCategoryId ?: return
        val normalizedName = name.trim()
        if (normalizedName.isBlank() || state.selectedCardIds.isEmpty()) return
        createContent { liveDoman.createRepertoire(normalizedName, categoryId, state.selectedCardIds) }
    }

    fun openRoom() {
        val groupId = _uiState.value.selectedGroupId ?: return
        val repertoireId = _uiState.value.selectedRepertoireId ?: return
        submit {
            val session = liveDoman.createSession(groupId, repertoireId)
            liveDoman.getSession(session.id)
        }
    }

    fun refreshRoom() {
        val id = _uiState.value.session?.id ?: return
        submit { liveDoman.getSession(id) }
    }

    fun command(action: String) {
        val current = _uiState.value.session ?: return
        submit {
            liveDoman.command(current.id, action, current.sequence, UUID.randomUUID().toString())
            liveDoman.getSession(current.id)
        }
    }

    private fun createContent(operation: suspend () -> Any) = viewModelScope.launch {
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        try {
            operation()
            _uiState.update { it.copy(isSubmitting = false, selectedCardIds = emptyList()) }
            loadSetup()
        } catch (error: Throwable) {
            _uiState.update { it.copy(isSubmitting = false, errorMessage = error.message ?: "No se pudo guardar el contenido.") }
        }
    }

    private fun submit(operation: suspend () -> LiveDomanSessionDetail) = viewModelScope.launch {
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        try {
            val detail = operation()
            _uiState.update { it.copy(isSubmitting = false, detail = detail) }
            subscribe(detail.session.id)
        } catch (error: Throwable) {
            _uiState.update { it.copy(isSubmitting = false, errorMessage = error.message ?: "No se pudo actualizar la sala.") }
        }
    }

    private fun subscribe(sessionId: String) {
        if (subscribedSessionId == sessionId) return
        viewModelScope.launch {
            subscription?.close()
            subscription = null
            subscribedSessionId = sessionId
            subscription = runCatching {
                liveDoman.subscribe(
                    sessionId = sessionId,
                    audience = "teacher",
                    onSession = { session -> _uiState.update { state -> state.copy(detail = state.detail?.copy(session = session)) } },
                    onParticipants = { participants -> _uiState.update { state -> state.copy(detail = state.detail?.copy(participants = participants)) } },
                    onConnection = { connected ->
                        _uiState.update { it.copy(isRealtimeConnected = connected) }
                        // Re-read authoritative state after the subscription is live.
                        if (connected) refreshRoom()
                    }
                )
            }.getOrElse {
                subscribedSessionId = null
                _uiState.update { it.copy(isRealtimeConnected = false) }
                null
            }
        }
    }

    override fun onCleared() {
        subscription?.close()
        super.onCleared()
    }
}

class TeacherLiveSessionViewModelFactory(
    private val groups: GroupsDataSource,
    private val liveDoman: LiveDomanDataSource,
    private val content: TeacherContentDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TeacherLiveSessionViewModel(groups, liveDoman, content) as T
}
