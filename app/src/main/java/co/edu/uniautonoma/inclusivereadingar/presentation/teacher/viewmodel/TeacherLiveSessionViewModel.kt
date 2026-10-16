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
    val categories: List<Category> = emptyList(),
    val cards: List<LiveDomanCard> = emptyList(),
    val selectedContentCategoryId: String? = null,
    val selectedCardIds: List<String> = emptyList(),
    val selectedGroupId: String? = null,
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
            val loadedCards = liveDoman.getCards()
            val loadedCategories = content.getCategories()
            _uiState.update { state ->
                val selectedCategoryId = state.selectedContentCategoryId
                    ?.takeIf { selected -> loadedCategories.any { it.id == selected } }
                    ?: loadedCategories.firstOrNull()?.id
                state.copy(
                    isLoading = false,
                    groups = loadedGroups,
                    categories = loadedCategories,
                    cards = loadedCards,
                    selectedGroupId = state.selectedGroupId ?: loadedGroups.firstOrNull()?.id,
                    selectedContentCategoryId = selectedCategoryId,
                    selectedCardIds = state.selectedCardIds.filter { selected -> loadedCards.any { it.id == selected } }
                )
            }
        } catch (error: Throwable) {
            _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
        }
    }

    fun selectGroup(id: String) = _uiState.update { it.copy(selectedGroupId = id) }
    fun selectContentCategory(id: String) = _uiState.update {
        it.copy(selectedContentCategoryId = id, selectedCardIds = emptyList())
    }

    fun toggleCard(id: String) = _uiState.update { state ->
        val selected = state.selectedCardIds
        when {
            id in selected -> state.copy(selectedCardIds = selected - id)
            selected.size < 10 -> state.copy(selectedCardIds = selected + id)
            else -> state.copy(errorMessage = "Puedes elegir máximo 10 tarjetas por sala.")
        }
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

    fun openRoom() {
        val state = _uiState.value
        val groupId = state.selectedGroupId ?: return
        val categoryId = state.selectedContentCategoryId ?: return
        if (state.selectedCardIds.isEmpty()) return
        val categoryName = state.categories.firstOrNull { it.id == categoryId }?.name ?: "Tarjetas"
        submit {
            // The backend keeps an immutable sequence snapshot through a repertoire,
            // but teachers should only need to choose and order cards in the UI.
            val sequence = liveDoman.createRepertoire(
                name = "Sala $categoryName - ${System.currentTimeMillis()}",
                categoryId = categoryId,
                cardIds = state.selectedCardIds
            )
            val session = liveDoman.createSession(groupId, sequence.id)
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
            _uiState.update { it.copy(isSubmitting = false) }
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
