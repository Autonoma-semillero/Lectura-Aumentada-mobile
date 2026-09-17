package co.edu.uniautonoma.inclusivereadingar.data.remote

import co.edu.uniautonoma.inclusivereadingar.domain.model.DomanSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.DomanSessionCard
import co.edu.uniautonoma.inclusivereadingar.domain.model.DomanSessionHistoryItem
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentProgressSummary
import org.json.JSONArray
import org.json.JSONObject

data class PlanSessionCounts(
    val total: Int,
    val completed: Int,
    val pending: Int,
    val inProgress: Int
)

interface DomanSessionsApi {
    suspend fun getNext(studentId: String, categoryId: String?, accessToken: String?): DomanSession
    suspend fun getSession(sessionId: String, accessToken: String?): DomanSession
    suspend fun start(sessionId: String, accessToken: String?): DomanSession
    suspend fun registerExposure(
        sessionId: String,
        wordCardId: String?,
        eventType: String,
        displayMs: Int?,
        accessToken: String?
    )
    suspend fun complete(sessionId: String, accessToken: String?): DomanSession
    suspend fun getByPlanId(planId: String, accessToken: String?): PlanSessionCounts
    suspend fun getHistory(studentId: String, accessToken: String?): List<DomanSessionHistoryItem>
    suspend fun getProgressSummary(studentId: String, accessToken: String?): StudentProgressSummary
}

class HttpDomanSessionsApi(
    private val httpClient: BackendHttpClient
) : DomanSessionsApi {
    override suspend fun getNext(
        studentId: String,
        categoryId: String?,
        accessToken: String?
    ): DomanSession {
        val params = buildMap {
            put("student_id", studentId)
            categoryId?.let { put("category_id", it) }
        }
        val response = httpClient.get(
            path = "/doman/sessions/next",
            queryParams = params,
            accessToken = accessToken
        )
        return parseSession(response)
    }

    override suspend fun getSession(sessionId: String, accessToken: String?): DomanSession {
        val response = httpClient.get(
            path = "/doman/sessions/$sessionId",
            accessToken = accessToken
        )
        return parseSession(response)
    }

    override suspend fun start(sessionId: String, accessToken: String?): DomanSession {
        val response = httpClient.post(
            path = "/doman/sessions/$sessionId/start",
            body = JSONObject(),
            accessToken = accessToken
        )
        return parseSession(response)
    }

    override suspend fun registerExposure(
        sessionId: String,
        wordCardId: String?,
        eventType: String,
        displayMs: Int?,
        accessToken: String?
    ) {
        val body = JSONObject().put("event_type", eventType)
        wordCardId?.let { body.put("word_card_id", it) }
        displayMs?.let { body.put("display_ms", it) }
        httpClient.post(
            path = "/doman/sessions/$sessionId/exposures",
            body = body,
            accessToken = accessToken
        )
    }

    override suspend fun getByPlanId(planId: String, accessToken: String?): PlanSessionCounts {
        val response = httpClient.get(
            path = "/doman/sessions",
            queryParams = mapOf("daily_plan_id" to planId),
            accessToken = accessToken
        )
        val json = JSONArray(response)
        var total = 0; var completed = 0; var pending = 0; var inProgress = 0
        for (i in 0 until json.length()) {
            total++
            when (json.getJSONObject(i).optString("status")) {
                "completed"   -> completed++
                "planned"     -> pending++
                "in_progress" -> inProgress++
            }
        }
        return PlanSessionCounts(total, completed, pending, inProgress)
    }

    override suspend fun complete(sessionId: String, accessToken: String?): DomanSession {
        val response = httpClient.post(
            path = "/doman/sessions/$sessionId/complete",
            body = JSONObject(),
            accessToken = accessToken
        )
        return parseSession(response)
    }

    override suspend fun getHistory(
        studentId: String,
        accessToken: String?
    ): List<DomanSessionHistoryItem> {
        val response = httpClient.get(
            path = "/doman/sessions/history",
            queryParams = mapOf("student_id" to studentId),
            accessToken = accessToken
        )
        val json = JSONArray(response)
        return buildList {
            for (index in 0 until json.length()) {
                val item = json.getJSONObject(index)
                add(
                    DomanSessionHistoryItem(
                        sessionId = item.getString("session_id"),
                        dailyPlanId = item.getString("daily_plan_id"),
                        categoryId = item.getString("category_id"),
                        sessionIndex = item.getInt("session_index"),
                        status = item.getString("status"),
                        displayMs = item.getInt("display_ms"),
                        startedAt = item.optString("started_at").ifBlank { null },
                        completedAt = item.optString("completed_at").ifBlank { null }
                    )
                )
            }
        }
    }

    override suspend fun getProgressSummary(
        studentId: String,
        accessToken: String?
    ): StudentProgressSummary {
        val response = httpClient.get(
            path = "/doman/progress/summary",
            queryParams = mapOf("student_id" to studentId),
            accessToken = accessToken
        )
        val json = JSONObject(response)
        return StudentProgressSummary(
            studentId = json.getString("student_id"),
            plannedSessionsCount = json.optInt("planned_sessions_count", 0),
            inProgressSessionsCount = json.optInt("in_progress_sessions_count", 0),
            completedSessionsCount = json.optInt("completed_sessions_count", 0),
            cardsNewCount = json.optInt("cards_new_count", 0),
            cardsActiveCount = json.optInt("cards_active_count", 0),
            cardsCompletedCount = json.optInt("cards_completed_count", 0),
            lastActivityAt = json.optString("last_activity_at").ifBlank { null }
        )
    }

    private fun parseSession(rawJson: String): DomanSession {
        val json = JSONObject(rawJson)
        val cardsArray = json.optJSONArray("cards") ?: JSONArray()
        val cards = buildList {
            for (index in 0 until cardsArray.length()) {
                val item = cardsArray.getJSONObject(index)
                add(
                    DomanSessionCard(
                        id = item.getString("id"),
                        orderIndex = item.optInt("order_index", index),
                        word = item.optString("word"),
                        status = item.optString("status", "active"),
                        audioUrl = item.optString("audio_url").ifBlank { null },
                        categoryId = item.optString("category_id").ifBlank { null },
                        learningUnitId = item.optString("learning_unit_id").ifBlank { null }
                    )
                )
            }
        }
        return DomanSession(
            sessionId = json.getString("session_id"),
            studentId = json.getString("student_id"),
            planId = json.getString("plan_id"),
            categoryId = json.getString("category_id"),
            sessionIndex = json.getInt("session_index"),
            status = json.getString("status"),
            displayMs = json.getInt("display_ms"),
            audioMode = json.optString("audio_mode", "manual"),
            mode = json.optString("mode", "auto"),
            cards = cards
        )
    }
}
