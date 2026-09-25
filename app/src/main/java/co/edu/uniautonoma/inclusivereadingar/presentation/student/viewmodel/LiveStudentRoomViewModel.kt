package co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.LiveDomanDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveStudentRoomUiState(
    val isLoading: Boolean = true,
    val session: LiveDomanSession? = null,
    val errorMessage: String? = null
)

/** Student rendering is deliberately passive: it only reflects teacher/server state. */
class LiveStudentRoomViewModel(
    private val liveDoman: LiveDomanDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(LiveStudentRoomUiState())
    val uiState: StateFlow<LiveStudentRoomUiState> = _uiState.asStateFlow()
    private var acknowledgedKey: String? = null

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching {
            liveDoman.getCurrent().firstOrNull()?.let { liveDoman.join(it.id) }
        }.onSuccess { session ->
            _uiState.value = LiveStudentRoomUiState(isLoading = false, session = session)
            acknowledgeIfShowing(session)
        }.onFailure { error ->
            _uiState.value = LiveStudentRoomUiState(
                isLoading = false,
                errorMessage = error.message ?: "No pudimos consultar la clase."
            )
        }
    }

    private fun acknowledgeIfShowing(session: LiveDomanSession?) {
        val current = session ?: return
        if (current.status != "running" || current.displayText.isNullOrBlank()) return
        val key = "${current.id}:${current.sequence}"
        if (key == acknowledgedKey) return
        acknowledgedKey = key
        viewModelScope.launch {
            runCatching { liveDoman.acknowledge(current.id, current.sequence) }
        }
    }
}

class LiveStudentRoomViewModelFactory(
    private val liveDoman: LiveDomanDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LiveStudentRoomViewModel(liveDoman) as T
}
