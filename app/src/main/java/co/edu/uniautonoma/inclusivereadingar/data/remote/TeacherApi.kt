package co.edu.uniautonoma.inclusivereadingar.data.remote

import co.edu.uniautonoma.inclusivereadingar.domain.model.AppUser
import co.edu.uniautonoma.inclusivereadingar.domain.model.Category
import co.edu.uniautonoma.inclusivereadingar.domain.model.WordCard
import org.json.JSONArray
import org.json.JSONObject

interface TeacherApi {
    suspend fun getCategories(accessToken: String?): List<Category>
    suspend fun createCategory(
        name: String,
        slug: String,
        description: String?,
        icon: String?,
        sortOrder: Int,
        accessToken: String?
    ): Category

    suspend fun updateCategory(
        id: String,
        name: String,
        slug: String,
        description: String?,
        icon: String?,
        sortOrder: Int,
        accessToken: String?
    ): Category

    suspend fun deleteCategory(id: String, accessToken: String?)
    suspend fun getUsers(accessToken: String?): List<AppUser>
    suspend fun createWordCard(
        studentId: String,
        word: String,
        categoryId: String,
        audioUrl: String?,
        accessToken: String?
    ): WordCard
}

class HttpTeacherApi(
    private val httpClient: BackendHttpClient
) : TeacherApi {
    override suspend fun getCategories(accessToken: String?): List<Category> {
        val response = httpClient.get(
            path = "/categories",
            accessToken = accessToken
        )
        return parseCategories(response)
    }

    override suspend fun createCategory(
        name: String,
        slug: String,
        description: String?,
        icon: String?,
        sortOrder: Int,
        accessToken: String?
    ): Category {
        val body = JSONObject()
            .put("name", name)
            .put("slug", slug)
            .put("sort_order", sortOrder)
        description?.let { body.put("description", it) }
        icon?.let { body.put("icon", it) }

        return parseCategory(
            httpClient.post(
                path = "/categories",
                body = body,
                accessToken = accessToken
            )
        )
    }

    override suspend fun updateCategory(
        id: String,
        name: String,
        slug: String,
        description: String?,
        icon: String?,
        sortOrder: Int,
        accessToken: String?
    ): Category {
        val body = JSONObject()
            .put("name", name)
            .put("slug", slug)
            .put("sort_order", sortOrder)
            .put("description", description ?: "")
            .put("icon", icon ?: "")

        return parseCategory(
            httpClient.requestRaw(
                method = "PATCH",
                path = "/categories/$id",
                body = body,
                accessToken = accessToken
            )
        )
    }

    override suspend fun deleteCategory(id: String, accessToken: String?) {
        httpClient.requestRaw(
            method = "DELETE",
            path = "/categories/$id",
            body = null,
            accessToken = accessToken
        )
    }

    override suspend fun getUsers(accessToken: String?): List<AppUser> {
        val response = httpClient.get(
            path = "/users",
            accessToken = accessToken
        )
        return parseUsers(response)
    }

    override suspend fun createWordCard(
        studentId: String,
        word: String,
        categoryId: String,
        audioUrl: String?,
        accessToken: String?
    ): WordCard {
        val body = JSONObject()
            .put("student_id", studentId)
            .put("word", word)
            .put("category_id", categoryId)
        audioUrl?.let { body.put("audio_url", it) }

        return parseWordCard(
            httpClient.post(
                path = "/word-cards",
                body = body,
                accessToken = accessToken
            )
        )
    }

    private fun parseCategories(rawJson: String): List<Category> {
        val json = JSONArray(rawJson)
        return buildList {
            for (index in 0 until json.length()) {
                add(parseCategory(json.getJSONObject(index).toString()))
            }
        }
    }

    private fun parseCategory(rawJson: String): Category {
        val item = JSONObject(rawJson)
        return Category(
            id = item.getString("id"),
            name = item.getString("name"),
            slug = item.getString("slug"),
            description = item.optString("description").ifBlank { null },
            icon = item.optString("icon").ifBlank { null },
            sortOrder = item.optInt("sort_order", 0),
            wordCardsCount = item.optInt("word_cards_count", 0)
        )
    }

    private fun parseUsers(rawJson: String): List<AppUser> {
        val json = JSONArray(rawJson)
        return buildList {
            for (index in 0 until json.length()) {
                val item = json.getJSONObject(index)
                val rolesJson = item.optJSONArray("roles")
                add(
                    AppUser(
                        id = item.getString("id"),
                        email = item.getString("email"),
                        displayName = item.optString("display_name").ifBlank { null },
                        roles = buildList {
                            if (rolesJson != null) {
                                for (roleIndex in 0 until rolesJson.length()) {
                                    add(rolesJson.getString(roleIndex))
                                }
                            }
                        }
                    )
                )
            }
        }
    }

    private fun parseWordCard(rawJson: String): WordCard {
        val item = JSONObject(rawJson)
        return WordCard(
            id = item.getString("id"),
            studentId = item.getString("student_id"),
            word = item.getString("word"),
            status = item.getString("status"),
            categoryId = item.optString("category_id").ifBlank { null },
            learningUnitId = item.optString("learning_unit_id").ifBlank { null },
            audioUrl = item.optString("audio_url").ifBlank { null },
            language = item.optString("language").ifBlank { null },
            initialLetter = item.optString("initial_letter"),
            timesShown = item.optInt("times_shown", 0),
            timesAudioPlayed = item.optInt("times_audio_played", -1).takeIf { it >= 0 }
        )
    }
}
