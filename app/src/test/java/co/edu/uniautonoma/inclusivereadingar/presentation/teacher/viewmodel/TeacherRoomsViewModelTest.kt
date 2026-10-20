package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import co.edu.uniautonoma.inclusivereadingar.MainDispatcherRule
import co.edu.uniautonoma.inclusivereadingar.data.repository.GroupsDataSource
import co.edu.uniautonoma.inclusivereadingar.data.repository.LiveDomanDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceSearchResult
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanRepertoire
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSessionDetail
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentGroup
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TeacherRoomsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun endRoomSendsTheEndCommandAndReloadsTheRoomsList() = runTest {
        val liveDoman = FakeLiveDomanDataSource(
            sessions = mutableListOf(
                session("room-1", status = "running", sequence = 3),
                session("room-2", status = "lobby", sequence = 0)
            )
        )
        val groups = FakeRoomsGroupsDataSource()
        val viewModel = TeacherRoomsViewModel(liveDoman, groups)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.rooms.map { it.id }).containsExactly("room-1", "room-2")

        viewModel.endRoom("room-1")
        advanceUntilIdle()

        assertThat(liveDoman.commands).containsExactly(FakeLiveDomanDataSource.Command("room-1", "end", 3))
        assertThat(viewModel.uiState.value.isSubmitting).isFalse()
    }

    @Test
    fun endRoomIsANoOpWhenTheRoomIsAlreadyFinished() = runTest {
        val liveDoman = FakeLiveDomanDataSource(sessions = mutableListOf(session("room-1", status = "ended", sequence = 5)))
        val viewModel = TeacherRoomsViewModel(liveDoman, FakeRoomsGroupsDataSource())
        advanceUntilIdle()

        viewModel.endRoom("room-1")
        advanceUntilIdle()

        assertThat(liveDoman.commands).isEmpty()
    }

    private fun session(id: String, status: String, sequence: Int) = LiveDomanSession(
        id = id,
        groupId = "group-1",
        repertoireId = "rep-1",
        repertoireName = "Sala",
        status = status,
        sequence = sequence,
        displayText = null,
        displayAudioUrl = null,
        displayDurationMs = 0
    )
}

private class FakeLiveDomanDataSource(
    private val sessions: MutableList<LiveDomanSession>
) : LiveDomanDataSource {
    data class Command(val sessionId: String, val action: String, val expectedSequence: Int)

    val commands = mutableListOf<Command>()

    override suspend fun getRepertoires(): List<LiveDomanRepertoire> = emptyList()
    override suspend fun createSession(groupId: String, repertoireId: String): LiveDomanSession = error("Not used")
    override suspend fun getCurrent(): List<LiveDomanSession> = sessions.toList()
    override suspend fun listSessions(): List<LiveDomanSession> = sessions.toList()
    override suspend fun getSession(sessionId: String): LiveDomanSessionDetail = error("Not used")
    override suspend fun join(sessionId: String): LiveDomanSession = error("Not used")
    override suspend fun command(sessionId: String, action: String, expectedSequence: Int, commandId: String): LiveDomanSession {
        commands += Command(sessionId, action, expectedSequence)
        val index = sessions.indexOfFirst { it.id == sessionId }
        val updated = sessions[index].copy(status = if (action == "end") "ended" else sessions[index].status)
        sessions[index] = updated
        return updated
    }
    override suspend fun acknowledge(sessionId: String, sequence: Int) = Unit
}

private class FakeRoomsGroupsDataSource : GroupsDataSource {
    override suspend fun getGroups(q: String?, status: String?): List<StudentGroup> = emptyList()
    override suspend fun searchAudience(q: String, limit: Int, unassigned: Boolean?): AudienceSearchResult =
        AudienceSearchResult(emptyList())
    override suspend fun createGroup(name: String, description: String?, studentIds: Collection<String>): StudentGroup =
        error("Not used")
    override suspend fun updateGroup(groupId: String, name: String, description: String?): StudentGroup =
        error("Not used")
    override suspend fun archiveGroup(groupId: String) = Unit
    override suspend fun addMembers(groupId: String, studentIds: Collection<String>): StudentGroup = error("Not used")
    override suspend fun removeMember(groupId: String, studentId: String): StudentGroup? = null
}
