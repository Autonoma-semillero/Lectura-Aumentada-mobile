package co.edu.uniautonoma.inclusivereadingar.data.repository

import co.edu.uniautonoma.inclusivereadingar.data.local.SessionStore
import co.edu.uniautonoma.inclusivereadingar.data.remote.GroupsApi
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceSearchResult
import co.edu.uniautonoma.inclusivereadingar.domain.model.AuthSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentGroup

interface GroupsDataSource {
    suspend fun getGroups(q: String? = null, status: String? = "active"): List<StudentGroup>
    suspend fun searchAudience(
        q: String,
        limit: Int = 50,
        unassigned: Boolean? = null
    ): AudienceSearchResult

    suspend fun createGroup(
        name: String,
        description: String?,
        studentIds: Collection<String>
    ): StudentGroup

    suspend fun updateGroup(groupId: String, name: String, description: String?): StudentGroup
    suspend fun archiveGroup(groupId: String)
    suspend fun addMembers(groupId: String, studentIds: Collection<String>): StudentGroup
    suspend fun removeMember(groupId: String, studentId: String): StudentGroup?
}

class GroupsRepository(
    private val sessionStore: SessionStore,
    private val groupsApi: GroupsApi
) : GroupsDataSource {
    override suspend fun getGroups(q: String?, status: String?): List<StudentGroup> {
        val session = requireSession()
        return groupsApi.getGroups(q, status, session.accessToken)
    }

    override suspend fun searchAudience(
        q: String,
        limit: Int,
        unassigned: Boolean?
    ): AudienceSearchResult {
        val session = requireSession()
        return groupsApi.searchAudience(q, limit, unassigned, session.accessToken)
    }

    override suspend fun createGroup(
        name: String,
        description: String?,
        studentIds: Collection<String>
    ): StudentGroup {
        val session = requireSession()
        return groupsApi.createGroup(name, description, studentIds, session.accessToken)
    }

    override suspend fun updateGroup(groupId: String, name: String, description: String?): StudentGroup {
        val session = requireSession()
        return groupsApi.updateGroup(groupId, name, description, session.accessToken)
    }

    override suspend fun archiveGroup(groupId: String) {
        val session = requireSession()
        groupsApi.archiveGroup(groupId, session.accessToken)
    }

    override suspend fun addMembers(groupId: String, studentIds: Collection<String>): StudentGroup {
        val session = requireSession()
        return groupsApi.addMembers(groupId, studentIds, session.accessToken)
    }

    override suspend fun removeMember(groupId: String, studentId: String): StudentGroup? {
        val session = requireSession()
        return groupsApi.removeMember(groupId, studentId, session.accessToken)
    }

    private suspend fun requireSession(): AuthSession = requireNotNull(sessionStore.getSession()) {
        "A valid session is required"
    }
}
