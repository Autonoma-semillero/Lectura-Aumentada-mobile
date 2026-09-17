package co.edu.uniautonoma.inclusivereadingar.data.remote

import co.edu.uniautonoma.inclusivereadingar.domain.model.AppUser
import co.edu.uniautonoma.inclusivereadingar.domain.model.CategoryStatusCounts
import co.edu.uniautonoma.inclusivereadingar.domain.model.CompletedCard
import co.edu.uniautonoma.inclusivereadingar.domain.model.CompletedCardsPage
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentCategoryProgress
import org.json.JSONArray
import org.json.JSONObject

interface DocenteApi {
    suspend fun getStudents(accessToken: String?): List<AppUser>
    suspend fun getStudentCategoryProgress(studentId: String, accessToken: String?): List<StudentCategoryProgress>
    suspend fun getStudentCompletedCards(
        studentId: String,
        categoryId: String?,
        page: Int,
        limit: Int,
        accessToken: String?
    ): CompletedCardsPage
}

class HttpDocenteApi(private val httpClient: BackendHttpClient) : DocenteApi {

    override suspend fun getStudents(accessToken: String?): List<AppUser> {
        val response = httpClient.get(path = "/docente/students", accessToken = accessToken)
        val json = JSONArray(response)
        return buildList {
            for (i in 0 until json.length()) {
                val item = json.getJSONObject(i)
                add(
                    AppUser(
                        id = item.getString("id"),
                        email = item.getString("email"),
                        displayName = item.optString("displayName").ifBlank { null },
                        roles = listOf("student")
                    )
                )
            }
        }
    }

    override suspend fun getStudentCategoryProgress(
        studentId: String,
        accessToken: String?
    ): List<StudentCategoryProgress> {
        val response = httpClient.get(
            path = "/docente/students/$studentId/progress",
            accessToken = accessToken
        )
        val json = JSONArray(response)
        return buildList {
            for (i in 0 until json.length()) {
                val item = json.getJSONObject(i)
                val byStatus = item.getJSONObject("byStatus")
                add(
                    StudentCategoryProgress(
                        categoryId = item.getString("categoryId"),
                        categoryName = item.getString("categoryName"),
                        categorySlug = item.getString("categorySlug"),
                        total = item.getInt("total"),
                        byStatus = CategoryStatusCounts(
                            new = byStatus.optInt("new", 0),
                            active = byStatus.optInt("active", 0),
                            completed = byStatus.optInt("completed", 0),
                            archived = byStatus.optInt("archived", 0)
                        ),
                        phase2Ready = item.optBoolean("phase2Ready", false)
                    )
                )
            }
        }
    }

    override suspend fun getStudentCompletedCards(
        studentId: String,
        categoryId: String?,
        page: Int,
        limit: Int,
        accessToken: String?
    ): CompletedCardsPage {
        val queryParams = mutableMapOf(
            "page" to page.toString(),
            "limit" to limit.toString()
        )
        if (categoryId != null) queryParams["categoryId"] = categoryId

        val response = httpClient.get(
            path = "/docente/students/$studentId/cards/completed",
            queryParams = queryParams,
            accessToken = accessToken
        )
        val json = JSONObject(response)
        val dataArray = json.getJSONArray("data")
        val cards = buildList {
            for (i in 0 until dataArray.length()) {
                val card = dataArray.getJSONObject(i)
                val category = card.getJSONObject("category")
                add(
                    CompletedCard(
                        id = card.getString("id"),
                        word = card.getString("word"),
                        audioUrl = card.optString("audioUrl").ifBlank { null },
                        categoryId = category.getString("id"),
                        categoryName = category.getString("name"),
                        timesShown = card.optInt("timesShown", 0),
                        timesAudioPlayed = card.optInt("timesAudioPlayed", 0),
                        completedAt = card.optString("completedAt").ifBlank { null }
                    )
                )
            }
        }
        return CompletedCardsPage(
            data = cards,
            total = json.getInt("total"),
            page = json.getInt("page"),
            limit = json.getInt("limit")
        )
    }
}
