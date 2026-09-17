package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import co.edu.uniautonoma.inclusivereadingar.MainDispatcherRule
import co.edu.uniautonoma.inclusivereadingar.data.repository.GroupsDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceSearchResult
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceStudent
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentGroup
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TeacherGroupsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun searchesOrphansAndEditorRemotelyUsingLatestQuery() = runTest {
        val repository = FakeGroupsDataSource()
        val viewModel = TeacherGroupsViewModel(repository)
        advanceUntilIdle()

        viewModel.onUnassignedQueryChange("a")
        viewModel.onUnassignedQueryChange("ana")
        advanceTimeBy(251)
        advanceUntilIdle()

        assertThat(repository.requests.last()).isEqualTo(SearchRequest("ana", true))
        assertThat(viewModel.uiState.value.unassignedResults.single().label).isEqualTo("Ana")

        viewModel.openCreate()
        viewModel.onEditorQueryChange("bea")
        advanceTimeBy(251)
        advanceUntilIdle()

        assertThat(repository.requests.last()).isEqualTo(SearchRequest("bea", null))
        assertThat(viewModel.uiState.value.editorResults.single().label).isEqualTo("Beatriz")
        assertThat(repository.requests.count { it.query == "a" }).isEqualTo(0)
    }
}

private data class SearchRequest(val query: String, val unassigned: Boolean?)

private class FakeGroupsDataSource : GroupsDataSource {
    val requests = mutableListOf<SearchRequest>()

    override suspend fun getGroups(q: String?, status: String?): List<StudentGroup> = emptyList()

    override suspend fun searchAudience(
        q: String,
        limit: Int,
        unassigned: Boolean?
    ): AudienceSearchResult {
        requests += SearchRequest(q, unassigned)
        val student = when (q) {
            "ana" -> student("s-ana", "Ana", true)
            "bea" -> student("s-bea", "Beatriz", false)
            else -> null
        }
        return AudienceSearchResult(listOfNotNull(student))
    }

    override suspend fun createGroup(
        name: String,
        description: String?,
        studentIds: Collection<String>
    ): StudentGroup = error("Not used")

    override suspend fun updateGroup(
        groupId: String,
        name: String,
        description: String?
    ): StudentGroup = error("Not used")

    override suspend fun archiveGroup(groupId: String) = Unit

    override suspend fun addMembers(
        groupId: String,
        studentIds: Collection<String>
    ): StudentGroup = error("Not used")

    override suspend fun removeMember(groupId: String, studentId: String): StudentGroup? = null

    private fun student(id: String, name: String, unassigned: Boolean) = AudienceStudent(
        audienceKey = "student:$id",
        id = id,
        displayName = name,
        email = "${name.lowercase()}@example.com",
        username = name.lowercase(),
        groupIds = if (unassigned) emptyList() else listOf("g1"),
        unassigned = unassigned
    )
}
