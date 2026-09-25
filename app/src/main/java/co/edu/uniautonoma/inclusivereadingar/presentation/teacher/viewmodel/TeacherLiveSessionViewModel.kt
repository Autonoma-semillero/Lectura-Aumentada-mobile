package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.GroupsDataSource
import co.edu.uniautonoma.inclusivereadingar.data.repository.LiveDomanDataSource
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
    val selectedGroupId: String? = null,
    val selectedRepertoireId: String? = null,
    val detail: LiveDomanSessionDetail? = null,
    val errorMessage: String? = null
) {
    val session: LiveDomanSession? get() = detail?.session
}

class TeacherLiveSessionViewModel(
    private val groups: GroupsDataSource,
    private val liveDoman: LiveDomanDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeacherLiveSessionUiState())
    val uiState: StateFlow<TeacherLiveSessionUiState> = _uiState.asStateFlow()

    init { loadSetup() }

    fun loadSetup() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching { groups.getGroups() to liveDoman.getRepertoires() }
            .onSuccess { (loadedGroups, repertoires) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        groups = loadedGroups,
                        repertoires = repertoires,
                        selectedGroupId = it.selectedGroupId ?: loadedGroups.firstOrNull()?.id,
                        selectedRepertoireId = it.selectedRepertoireId ?: repertoires.firstOrNull()?.id
                    )
                }
            }
            .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
    }

    fun selectGroup(id: String) = _uiState.update { it.copy(selectedGroupId = id) }
    fun selectRepertoire(id: String) = _uiState.update { it.copy(selectedRepertoireId = id) }

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

    private fun submit(operation: suspend () -> LiveDomanSessionDetail) = viewModelScope.launch {
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        runCatching(operation)
            .onSuccess { detail -> _uiState.update { it.copy(isSubmitting = false, detail = detail) } }
            .onFailure { error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = error.message ?: "No se pudo actualizar la sala.") } }
    }
}

class TeacherLiveSessionViewModelFactory(
    private val groups: GroupsDataSource,
    private val liveDoman: LiveDomanDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TeacherLiveSessionViewModel(groups, liveDoman) as T
}
