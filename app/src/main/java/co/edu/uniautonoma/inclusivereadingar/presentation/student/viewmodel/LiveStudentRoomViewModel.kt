package co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.LiveDomanDataSource
import co.edu.uniautonoma.inclusivereadingar.data.remote.LiveDomanRealtimeSubscription
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveStudentRoomUiState(
    val isLoading: Boolean = true,
    val session: LiveDomanSession? = null,
    val errorMessage: String? = null,
    val isRealtimeConnected: Boolean = false,
    val isLocallyPaused: Boolean = false
)

/** Student rendering is deliberately passive: it only reflects teacher/server state. */
class LiveStudentRoomViewModel(
    private val liveDoman: LiveDomanDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(LiveStudentRoomUiState())
    val uiState: StateFlow<LiveStudentRoomUiState> = _uiState.asStateFlow()
    private var acknowledgedKey: String? = null
    private var roomSubscription: LiveDomanRealtimeSubscription? = null
    private var feedSubscription: LiveDomanRealtimeSubscription? = null
    private var roomSessionId: String? = null
    private var roomWasConnected = false
    private var feedWasConnected = false
    private var heartbeatJob: Job? = null
    private var fallbackSyncJob: Job? = null

    init {
        refresh()
        startFallbackSync()
    }

    fun refresh(): Job = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching {
            if (feedSubscription == null) subscribeToFeed()
            val sessions = liveDoman.getCurrent()
            val selected = sessions.firstOrNull { it.id == roomSessionId }
                ?: sessions.firstOrNull { it.status == "running" || it.status == "paused" }
                ?: sessions.firstOrNull()
            selected?.let { liveDoman.join(it.id) }
        }.onSuccess { session ->
            _uiState.update { state -> state.copy(isLoading = false, session = session, errorMessage = null) }
            if (session != null) {
                subscribe(session.id)
                startHeartbeat(session.id)
                acknowledgeIfShowing(session)
            }
        }.onFailure { error ->
            _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "No pudimos consultar la clase.") }
        }
    }

    fun toggleLocalPause() = _uiState.update { it.copy(isLocallyPaused = !it.isLocallyPaused) }

    private fun subscribe(sessionId: String) {
        if (roomSessionId == sessionId) return
        viewModelScope.launch {
            roomSubscription?.close()
            roomSubscription = null
            roomSessionId = sessionId
            roomSubscription = runCatching {
                liveDoman.subscribe(
                    sessionId = sessionId,
                    audience = "student",
                    onSession = ::applyRealtimeSession,
                    onConnection = { connected ->
                        _uiState.update { state -> state.copy(isRealtimeConnected = connected) }
                        // A read after every successful (re)connection closes the gap between
                        // the last REST response and the Ably subscription becoming active.
                        if (connected) refresh()
                        if (connected) _uiState.value.session?.let(::acknowledgeIfShowing)
                        roomWasConnected = connected
                    }
                )
            }.getOrElse {
                _uiState.update { state -> state.copy(isRealtimeConnected = false) }
                roomSessionId = null
                null
            }
        }
    }

    private fun subscribeToFeed(): Job = viewModelScope.launch {
        feedSubscription = runCatching {
            liveDoman.subscribeStudentFeed(
                onRoomOpened = ::joinOpenedRoom,
                onConnection = { connected ->
                    _uiState.update { it.copy(isRealtimeConnected = connected) }
                    if (connected && feedWasConnected) refresh()
                    feedWasConnected = connected
                }
            )
        }.getOrElse { null }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun joinOpenedRoom(sessionId: String): Job = viewModelScope.launch {
        // The server decides which valid room should be foregrounded. Do not let a new lobby
        // displace an already-running room locally.
        refresh()
    }

    private fun applyRealtimeSession(session: LiveDomanSession) {
        val current = _uiState.value.session
        if (current != null && session.sequence < current.sequence) return
        _uiState.update { it.copy(isLoading = false, session = session, errorMessage = null) }
        if (session.status == "ended" || session.status == "cancelled") heartbeatJob?.cancel()
        acknowledgeIfShowing(session)
    }

    private fun startHeartbeat(sessionId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (true) {
                runCatching { liveDoman.heartbeat(sessionId) }
                delay(30_000)
            }
        }
    }

    /**
     * Ably remains the low-latency path. When it cannot connect, this loop keeps
     * the student's screen faithful to the server instead of freezing on the
     * first card. It also discovers rooms opened while the student is waiting.
     */
    private fun startFallbackSync() {
        fallbackSyncJob?.cancel()
        fallbackSyncJob = viewModelScope.launch {
            while (true) {
                if (!_uiState.value.isRealtimeConnected) {
                    val current = _uiState.value.session
                    if (current != null) {
                        runCatching { liveDoman.getSession(current.id).session }
                            .onSuccess(::applyRealtimeSession)
                    } else {
                        runCatching {
                            val sessions = liveDoman.getCurrent()
                            sessions.firstOrNull { it.status == "running" || it.status == "paused" }
                                ?: sessions.firstOrNull()
                        }.getOrNull()?.let { discovered ->
                            runCatching { liveDoman.join(discovered.id) }
                                .onSuccess { joined ->
                                    _uiState.update { it.copy(isLoading = false, session = joined, errorMessage = null) }
                                    subscribe(joined.id)
                                    startHeartbeat(joined.id)
                                    acknowledgeIfShowing(joined)
                                }
                        }
                    }
                }
                delay(2_000)
            }
        }
    }

    private fun acknowledgeIfShowing(session: LiveDomanSession?) {
        val current = session ?: return
        if (current.status != "running" || current.displayText.isNullOrBlank()) return
        val key = "${current.id}:${current.sequence}"
        if (key == acknowledgedKey) return
        viewModelScope.launch {
            runCatching { liveDoman.acknowledge(current.id, current.sequence) }
                .onSuccess { acknowledgedKey = key }
        }
    }

    override fun onCleared() {
        roomSubscription?.close()
        feedSubscription?.close()
        heartbeatJob?.cancel()
        fallbackSyncJob?.cancel()
        roomSubscription = null
        feedSubscription = null
        fallbackSyncJob = null
        super.onCleared()
    }
}

class LiveStudentRoomViewModelFactory(
    private val liveDoman: LiveDomanDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LiveStudentRoomViewModel(liveDoman) as T
}
