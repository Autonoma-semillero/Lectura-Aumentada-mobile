package co.edu.uniautonoma.inclusivereadingar.data.remote

import co.edu.uniautonoma.inclusivereadingar.domain.model.CategorySummary
import org.json.JSONArray

interface CategoriesApi {
    suspend fun getAvailableCategories(studentId: String, accessToken: String?): List<CategorySummary>
}

class HttpCategoriesApi(
    private val httpClient: BackendHttpClient
) : CategoriesApi {
    override suspend fun getAvailableCategories(
        studentId: String,
        accessToken: String?
    ): List<CategorySummary> {
        val response = httpClient.get(
            path = "/categories/with-available-word-cards",
            queryParams = mapOf("student_id" to studentId),
            accessToken = accessToken
        )
        return parseCategories(response)
    }

    private fun parseCategories(rawJson: String): List<CategorySummary> {
        val json = JSONArray(rawJson)
        return buildList {
            for (index in 0 until json.length()) {
                val item = json.getJSONObject(index)
                add(
                    CategorySummary(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        slug = item.getString("slug"),
                        description = item.optString("description").ifBlank { null },
                        availableWordCardsCount = item.optInt("available_word_cards_count", 0)
                    )
                )
            }
        }
    }
}
