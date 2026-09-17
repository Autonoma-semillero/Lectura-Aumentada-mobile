package co.edu.uniautonoma.inclusivereadingar.data.remote

import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceGroup
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceSearchItem
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceSearchResult
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceStudent
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentGroup
import org.json.JSONArray
import org.json.JSONObject

interface GroupsApi {
    suspend fun getGroups(q: String?, status: String?, accessToken: String?): List<StudentGroup>
    suspend fun searchAudience(
        q: String,
        limit: Int,
        unassigned: Boolean?,
        accessToken: String?
    ): AudienceSearchResult

    suspend fun createGroup(
        name: String,
        description: String?,
        studentIds: Collection<String>,
        accessToken: String?
    ): StudentGroup

    suspend fun updateGroup(
        groupId: String,
        name: String,
        description: String?,
        accessToken: String?
    ): StudentGroup

    suspend fun archiveGroup(groupId: String, accessToken: String?)
    suspend fun addMembers(groupId: String, studentIds: Collection<String>, accessToken: String?): StudentGroup
    suspend fun removeMember(groupId: String, studentId: String, accessToken: String?): StudentGroup?
}

class HttpGroupsApi(
    private val httpClient: BackendHttpClient
) : GroupsApi {
    override suspend fun getGroups(
        q: String?,
        status: String?,
        accessToken: String?
    ): List<StudentGroup> {
        val params = buildMap {
            q?.trim()?.takeIf { it.isNotBlank() }?.let { put("q", it) }
            status?.takeIf { it.isNotBlank() }?.let { put("status", it) }
        }
        val raw = httpClient.get("/groups", params, accessToken)
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                add(parseStudentGroup(array.getJSONObject(index)))
            }
        }
    }

    override suspend fun searchAudience(
        q: String,
        limit: Int,
        unassigned: Boolean?,
        accessToken: String?
    ): AudienceSearchResult {
        val params = buildMap {
            put("q", q.trim())
            put("limit", limit.coerceIn(1, 100).toString())
            unassigned?.let { put("unassigned", it.toString()) }
        }
        return parseAudienceSearch(
            httpClient.get("/groups/audience/search", params, accessToken)
        )
    }

    override suspend fun createGroup(
        name: String,
        description: String?,
        studentIds: Collection<String>,
        accessToken: String?
    ): StudentGroup {
        val body = JSONObject()
            .put("name", name.trim())
            .put("student_ids", studentIds.toJsonArray())
        description?.trim()?.takeIf { it.isNotBlank() }?.let { body.put("description", it) }
        return parseStudentGroup(
            JSONObject(httpClient.post("/groups", body, accessToken))
        )
    }

    override suspend fun updateGroup(
        groupId: String,
        name: String,
        description: String?,
        accessToken: String?
    ): StudentGroup {
        val body = JSONObject()
            .put("name", name.trim())
            .put("description", description?.trim().orEmpty())
        return parseStudentGroup(
            JSONObject(
                httpClient.requestRaw(
                    method = "PATCH",
                    path = "/groups/$groupId",
                    body = body,
                    accessToken = accessToken
                )
            )
        )
    }

    override suspend fun archiveGroup(groupId: String, accessToken: String?) {
        httpClient.requestRaw(
            method = "DELETE",
            path = "/groups/$groupId",
            accessToken = accessToken
        )
    }

    override suspend fun addMembers(
        groupId: String,
        studentIds: Collection<String>,
        accessToken: String?
    ): StudentGroup {
        val raw = httpClient.post(
            path = "/groups/$groupId/members",
            body = JSONObject().put("student_ids", studentIds.toJsonArray()),
            accessToken = accessToken
        )
        return parseStudentGroup(JSONObject(raw))
    }

    override suspend fun removeMember(
        groupId: String,
        studentId: String,
        accessToken: String?
    ): StudentGroup? {
        val raw = httpClient.requestRaw(
            method = "DELETE",
            path = "/groups/$groupId/members/$studentId",
            accessToken = accessToken
        )
        if (raw.isBlank()) return null
        return runCatching { parseStudentGroup(JSONObject(raw)) }.getOrNull()
    }
}

internal fun parseStudentGroup(item: JSONObject): StudentGroup = StudentGroup(
    id = item.getString("id"),
    name = item.getString("name"),
    normalizedName = item.optString("normalized_name").ifBlank { item.getString("name").lowercase() },
    description = item.optNullableString("description"),
    teacherId = item.optString("teacher_id"),
    status = item.optString("status").ifBlank { "active" },
    createdBy = item.optString("created_by"),
    studentIds = item.optJSONArray("student_ids").toStringList(),
    createdAt = item.optNullableString("created_at"),
    updatedAt = item.optNullableString("updated_at")
)

internal fun parseAudienceSearch(raw: String): AudienceSearchResult {
    val root = JSONObject(raw)
    val items = root.optJSONArray("items") ?: JSONArray()
    val parsed = buildList<AudienceSearchItem> {
        for (index in 0 until items.length()) {
            val item = items.getJSONObject(index)
            when (item.optString("type")) {
                "student" -> add(
                    AudienceStudent(
                        audienceKey = item.optString("audience_key").ifBlank { "student:${item.getString("id")}" },
                        id = item.getString("id"),
                        displayName = item.optNullableString("display_name"),
                        email = item.optString("email"),
                        username = item.optNullableString("username"),
                        groupIds = item.optJSONArray("group_ids").toStringList(),
                        unassigned = item.optBoolean("unassigned", false)
                    )
                )

                "group" -> add(
                    AudienceGroup(
                        audienceKey = item.optString("audience_key").ifBlank { "group:${item.getString("id")}" },
                        id = item.getString("id"),
                        name = item.getString("name"),
                        description = item.optNullableString("description"),
                        teacherId = item.optString("teacher_id"),
                        status = item.optString("status").ifBlank { "active" },
                        studentIds = item.optJSONArray("student_ids").toStringList()
                    )
                )
            }
        }
    }
    return AudienceSearchResult(parsed)
}

private fun Collection<String>.toJsonArray(): JSONArray = JSONArray().also { array ->
    forEach(array::put)
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}

private fun JSONObject.optNullableString(name: String): String? {
    if (!has(name) || isNull(name)) return null
    return optString(name).takeIf { it.isNotBlank() }
}
