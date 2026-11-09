package co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel

import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.MainDispatcherRule
import co.edu.uniautonoma.inclusivereadingar.data.remote.LiveDomanRealtimeSubscription
import co.edu.uniautonoma.inclusivereadingar.data.remote.NoopLiveDomanRealtimeSubscription
import co.edu.uniautonoma.inclusivereadingar.data.repository.LiveDomanDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanRepertoire
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSessionDetail
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveStudentRoomViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun concurrentRefreshCannotLetAnOlderEmptyResponseHideAStartedRoom() = runTest {
        val startedRoom = session("room-1")
        val firstResponse = CompletableDeferred<List<LiveDomanSession>>()
        val liveDoman = RacingLiveDomanDataSource(firstResponse, startedRoom)
        val viewModel = LiveStudentRoomViewModel(liveDoman, startBackgroundSync = false)
        try {
            runCurrent()

            viewModel.refresh()
            runCurrent()
            firstResponse.complete(emptyList())
            runCurrent()

            assertThat(viewModel.uiState.value.session).isEqualTo(startedRoom)
            assertThat(viewModel.uiState.value.isLoading).isFalse()
            assertThat(liveDoman.joinedSessionIds).containsExactly("room-1")
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun endedRoomReturnsStudentToTheWaitingScreenState() = runTest {
        val startedRoom = session("room-1")
        val liveDoman = EndingRoomDataSource(startedRoom)
        val viewModel = LiveStudentRoomViewModel(liveDoman, startBackgroundSync = false)
        try {
            runCurrent()
            assertThat(viewModel.uiState.value.session).isEqualTo(startedRoom)

            liveDoman.emit(startedRoom.copy(status = "ended", sequence = 2))
            runCurrent()

            assertThat(viewModel.uiState.value.session).isNull()
            assertThat(viewModel.uiState.value.isLocallyPaused).isFalse()
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    private fun session(id: String) = LiveDomanSession(
        id = id,
        groupId = "group-1",
        repertoireId = "rep-1",
        repertoireName = "Animales",
        status = "running",
        sequence = 1,
        displayText = "GATO",
        displayAudioUrl = null,
        displayDurationMs = 3_000,
    )
}

private class EndingRoomDataSource(private val room: LiveDomanSession) : LiveDomanDataSource {
    private var onSession: ((LiveDomanSession) -> Unit)? = null

    fun emit(session: LiveDomanSession) = requireNotNull(onSession).invoke(session)

    override suspend fun getRepertoires(): List<LiveDomanRepertoire> = emptyList()
    override suspend fun createSession(groupId: String, repertoireId: String): LiveDomanSession = error("Not used")
    override suspend fun getCurrent(): List<LiveDomanSession> = listOf(room)
    override suspend fun listSessions(): List<LiveDomanSession> = listOf(room)
    override suspend fun getSession(sessionId: String): LiveDomanSessionDetail = LiveDomanSessionDetail(room, emptyList())
    override suspend fun join(sessionId: String): LiveDomanSession = room
    override suspend fun command(sessionId: String, action: String, expectedSequence: Int, commandId: String): LiveDomanSession = error("Not used")
    override suspend fun acknowledge(sessionId: String, sequence: Int) = Unit
    override suspend fun subscribe(
        sessionId: String,
        audience: String,
        onSession: (LiveDomanSession) -> Unit,
        onParticipants: (List<co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanParticipant>) -> Unit,
        onConnection: (Boolean) -> Unit,
    ): LiveDomanRealtimeSubscription {
        this.onSession = onSession
        return NoopLiveDomanRealtimeSubscription
    }
}

private class RacingLiveDomanDataSource(
    private val firstResponse: CompletableDeferred<List<LiveDomanSession>>,
    private val startedRoom: LiveDomanSession,
) : LiveDomanDataSource {
    private var currentCalls = 0
    val joinedSessionIds = mutableListOf<String>()

    override suspend fun getRepertoires(): List<LiveDomanRepertoire> = emptyList()
    override suspend fun createSession(groupId: String, repertoireId: String): LiveDomanSession = error("Not used")
    override suspend fun getCurrent(): List<LiveDomanSession> =
        if (currentCalls++ == 0) firstResponse.await() else listOf(startedRoom)
    override suspend fun listSessions(): List<LiveDomanSession> = emptyList()
    override suspend fun getSession(sessionId: String): LiveDomanSessionDetail = LiveDomanSessionDetail(startedRoom, emptyList())
    override suspend fun join(sessionId: String): LiveDomanSession {
        joinedSessionIds += sessionId
        return startedRoom
    }
    override suspend fun command(sessionId: String, action: String, expectedSequence: Int, commandId: String): LiveDomanSession = error("Not used")
    override suspend fun acknowledge(sessionId: String, sequence: Int) = Unit
}
