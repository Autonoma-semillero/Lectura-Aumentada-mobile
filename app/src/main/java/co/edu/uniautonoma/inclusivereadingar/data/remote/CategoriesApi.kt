package co.edu.uniautonoma.inclusivereadingar.data.remote

import co.edu.uniautonoma.inclusivereadingar.domain.model.CategorySummary
import org.json.JSONArray
import org.json.JSONObject

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
        val configuredCategories = runCatching {
            val response = httpClient.get(
                path = "/doman/study-plans/active",
                queryParams = mapOf("student_id" to studentId),
                accessToken = accessToken
            )
            parseStudyPlanCategories(response)
        }.getOrNull()
        if (configuredCategories != null) {
            return configuredCategories
        }

        val response = httpClient.get(
            path = "/categories/with-available-word-cards",
            queryParams = mapOf("student_id" to studentId),
            accessToken = accessToken
        )
        return parseCategories(response)
    }

    /**
     * `null` means that the student has no active study plan, so the legacy
     * category catalogue remains available. An empty list means that a plan
     * exists but has no level scheduled for today.
     */
    private fun parseStudyPlanCategories(rawJson: String): List<CategorySummary>? {
        val json = JSONObject(rawJson)
        if (json.isNull("plan_id")) return null
        return parseCategories(json.optJSONArray("categories") ?: JSONArray())
    }

    private fun parseCategories(rawJson: String): List<CategorySummary> =
        parseCategories(JSONArray(rawJson))

    private fun parseCategories(json: JSONArray): List<CategorySummary> {
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
