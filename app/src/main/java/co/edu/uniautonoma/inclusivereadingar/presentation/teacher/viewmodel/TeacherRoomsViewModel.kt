package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.GroupsDataSource
import co.edu.uniautonoma.inclusivereadingar.data.repository.LiveDomanDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSession
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val ACTIVE_STATUSES = setOf("lobby", "running", "paused")

data class TeacherRoomsUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val rooms: List<LiveDomanSession> = emptyList(),
    val groupNames: Map<String, String> = emptyMap(),
    val errorMessage: String? = null
)

class TeacherRoomsViewModel(
    private val liveDoman: LiveDomanDataSource,
    private val groups: GroupsDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeacherRoomsUiState())
    val uiState: StateFlow<TeacherRoomsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val sessions = liveDoman.listSessions()
            val loadedGroups = runCatching { groups.getGroups() }.getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    rooms = sessions,
                    groupNames = loadedGroups.associate { group -> group.id to group.name }
                )
            }
        } catch (error: Throwable) {
            _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "No se pudieron cargar las salas.") }
        }
    }

    fun endRoom(sessionId: String) = viewModelScope.launch {
        val current = _uiState.value.rooms.firstOrNull { it.id == sessionId } ?: return@launch
        if (current.status !in ACTIVE_STATUSES) return@launch
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        try {
            liveDoman.command(sessionId, "end", current.sequence, UUID.randomUUID().toString())
            val sessions = liveDoman.listSessions()
            _uiState.update { it.copy(isSubmitting = false, rooms = sessions) }
        } catch (error: Throwable) {
            _uiState.update { it.copy(isSubmitting = false, errorMessage = error.message ?: "No se pudo finalizar la sala.") }
        }
    }
}

class TeacherRoomsViewModelFactory(
    private val liveDoman: LiveDomanDataSource,
    private val groups: GroupsDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TeacherRoomsViewModel(liveDoman, groups) as T
}
