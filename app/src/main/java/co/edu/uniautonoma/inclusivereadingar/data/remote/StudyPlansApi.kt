package co.edu.uniautonoma.inclusivereadingar.data.remote

import co.edu.uniautonoma.inclusivereadingar.domain.model.ActiveStudyPlan
import co.edu.uniautonoma.inclusivereadingar.domain.model.ActiveStudyPlanCategory
import org.json.JSONObject

interface StudyPlansApi {
    suspend fun getActive(studentId: String, date: String?, accessToken: String?): ActiveStudyPlan?
}

class HttpStudyPlansApi(
    private val httpClient: BackendHttpClient
) : StudyPlansApi {
    override suspend fun getActive(
        studentId: String,
        date: String?,
        accessToken: String?
    ): ActiveStudyPlan? {
        val queryParams = buildMap {
            put("student_id", studentId)
            date?.let { put("date", it) }
        }
        val response = httpClient.get(
            path = "/doman/study-plans/active",
            queryParams = queryParams,
            accessToken = accessToken
        )
        return parseActiveStudyPlan(JSONObject(response))
    }
}

internal fun parseActiveStudyPlan(json: JSONObject): ActiveStudyPlan? {
    if (json.isNull("plan_id") || json.optString("plan_id").isBlank()) {
        return null
    }
    val categoriesArray = json.optJSONArray("categories")
    val categories = buildList {
        if (categoriesArray != null) {
            for (index in 0 until categoriesArray.length()) {
                val category = categoriesArray.getJSONObject(index)
                add(
                    ActiveStudyPlanCategory(
                        id = category.getString("id"),
                        name = category.getString("name"),
                        slug = category.optString("slug").ifBlank { null },
                        description = category.optString("description").ifBlank { null },
                        icon = category.optString("icon").ifBlank { null },
                        availableWordCardsCount = category.optInt("available_word_cards_count", 0)
                    )
                )
            }
        }
    }
    return ActiveStudyPlan(
        planId = json.getString("plan_id"),
        planName = json.optString("plan_name").ifBlank { null },
        levelId = if (json.isNull("level_id")) null else json.optString("level_id").ifBlank { null },
        levelName = json.optString("level_name").ifBlank { null },
        date = json.optString("date").ifBlank { null },
        categories = categories
    )
}
