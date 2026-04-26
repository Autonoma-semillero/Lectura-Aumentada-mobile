package co.edu.uniautonoma.inclusivereadingar.data.remote

import co.edu.uniautonoma.inclusivereadingar.domain.model.DailyPlanSummary
import org.json.JSONObject

interface DomanPlansApi {
    suspend fun getToday(studentId: String, accessToken: String?): DailyPlanSummary
    suspend fun generate(
        studentId: String,
        categoryId: String?,
        force: Boolean,
        accessToken: String?
    ): DailyPlanSummary
}

class HttpDomanPlansApi(
    private val httpClient: BackendHttpClient
) : DomanPlansApi {
    override suspend fun getToday(studentId: String, accessToken: String?): DailyPlanSummary {
        val response = httpClient.get(
            path = "/doman/daily-plans/today",
            queryParams = mapOf("student_id" to studentId),
            accessToken = accessToken
        )
        return parsePlanSummary(response)
    }

    override suspend fun generate(
        studentId: String,
        categoryId: String?,
        force: Boolean,
        accessToken: String?
    ): DailyPlanSummary {
        val body = JSONObject()
            .put("student_id", studentId)
            .put("force", force)
        categoryId?.let { body.put("category_id", it) }
        val response = httpClient.post(
            path = "/doman/daily-plans/generate",
            body = body,
            accessToken = accessToken
        )
        return parsePlanSummary(response)
    }

    private fun parsePlanSummary(rawJson: String): DailyPlanSummary {
        val json = JSONObject(rawJson)
        val plan = json.getJSONObject("plan")
        val cardsArray = json.optJSONArray("cards")
        val words = buildList {
            if (cardsArray != null) {
                for (index in 0 until cardsArray.length()) {
                    add(cardsArray.getJSONObject(index).optString("word"))
                }
            }
        }.filter { it.isNotBlank() }

        return DailyPlanSummary(
            planId = plan.getString("id"),
            studentId = plan.getString("student_id"),
            categoryId = plan.getString("category_id"),
            targetCardsCount = plan.optInt("target_cards_count", words.size),
            targetSessionsCount = plan.optInt("target_sessions_count", 0),
            cardsCount = json.optInt("cards_count", words.size),
            sessionsCount = json.optInt("sessions_count", 0),
            pendingSessionsCount = json.optInt("pending_sessions_count", 0),
            completedSessionsCount = json.optInt("completed_sessions_count", 0),
            nextSessionId = json.optString("next_session_id").ifBlank { null },
            words = words
        )
    }
}
