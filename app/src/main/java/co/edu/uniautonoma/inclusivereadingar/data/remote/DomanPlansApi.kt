package co.edu.uniautonoma.inclusivereadingar.data.remote

import co.edu.uniautonoma.inclusivereadingar.domain.model.DailyPlanSummary
import co.edu.uniautonoma.inclusivereadingar.domain.model.BulkPlanAudience
import co.edu.uniautonoma.inclusivereadingar.domain.model.BulkPlanAudienceStudent
import co.edu.uniautonoma.inclusivereadingar.domain.model.BulkPlanGenerationResult
import co.edu.uniautonoma.inclusivereadingar.domain.model.BulkPlanItemResult
import co.edu.uniautonoma.inclusivereadingar.domain.model.BulkPlanStatus
import co.edu.uniautonoma.inclusivereadingar.domain.model.BulkPlanSummary
import org.json.JSONArray
import org.json.JSONObject

interface DomanPlansApi {
    suspend fun getToday(studentId: String, accessToken: String?): DailyPlanSummary
    suspend fun generate(
        studentId: String,
        categoryId: String?,
        force: Boolean,
        accessToken: String?
    ): DailyPlanSummary
    suspend fun generateBulk(
        groupIds: Collection<String>,
        studentIds: Collection<String>,
        categoryId: String?,
        planDate: String? = null,
        targetCardsCount: Int? = null,
        targetSessionsCount: Int? = null,
        displayMs: Int? = null,
        force: Boolean = false,
        accessToken: String?
    ): BulkPlanGenerationResult
    suspend fun getByDateRange(
        studentId: String,
        from: String,
        to: String,
        accessToken: String?
    ): List<DailyPlanSummary>
    suspend fun deletePlan(planId: String, accessToken: String?)
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

    override suspend fun generateBulk(
        groupIds: Collection<String>,
        studentIds: Collection<String>,
        categoryId: String?,
        planDate: String?,
        targetCardsCount: Int?,
        targetSessionsCount: Int?,
        displayMs: Int?,
        force: Boolean,
        accessToken: String?
    ): BulkPlanGenerationResult {
        val body = JSONObject()
            .put("group_ids", groupIds.toJsonArray())
            .put("student_ids", studentIds.toJsonArray())
            .put("force", force)
        categoryId?.let { body.put("category_id", it) }
        planDate?.let { body.put("plan_date", it) }
        targetCardsCount?.let { body.put("target_cards_count", it) }
        targetSessionsCount?.let { body.put("target_sessions_count", it) }
        displayMs?.let { body.put("display_ms", it) }
        return parseBulkPlanGenerationResult(
            httpClient.post(
                path = "/doman/daily-plans/bulk-generate",
                body = body,
                accessToken = accessToken
            )
        )
    }

    override suspend fun getByDateRange(
        studentId: String,
        from: String,
        to: String,
        accessToken: String?
    ): List<DailyPlanSummary> {
        val response = httpClient.get(
            path = "/doman/daily-plans",
            queryParams = mapOf("student_id" to studentId, "from" to from, "to" to to),
            accessToken = accessToken
        )
        val jsonArray = JSONArray(response)
        return buildList {
            for (index in 0 until jsonArray.length()) {
                add(parsePlanFromList(jsonArray.getJSONObject(index)))
            }
        }
    }

    override suspend fun deletePlan(planId: String, accessToken: String?) {
        httpClient.requestRaw(
            method = "DELETE",
            path = "/doman/daily-plans/$planId",
            body = null,
            accessToken = accessToken
        )
    }

    private fun parsePlanFromList(json: JSONObject): DailyPlanSummary {
        val cardsCount = json.optInt("target_cards_count", 0)
        val sessionsCount = json.optInt("target_sessions_count", 0)
        return DailyPlanSummary(
            planId = json.getString("id"),
            studentId = json.getString("student_id"),
            categoryId = json.getString("category_id"),
            targetCardsCount = cardsCount,
            targetSessionsCount = sessionsCount,
            cardsCount = cardsCount,
            sessionsCount = sessionsCount,
            pendingSessionsCount = 0,
            completedSessionsCount = 0,
            nextSessionId = null,
            words = emptyList()
        )
    }

    private fun parsePlanSummary(rawJson: String): DailyPlanSummary {
        return parseDailyPlanSummary(JSONObject(rawJson))
    }
}

internal fun parseDailyPlanSummary(json: JSONObject): DailyPlanSummary {
        // Handle both { "plan": {...}, "cards": [...] } and flat plan object
        val plan = if (json.has("plan")) json.getJSONObject("plan") else json
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

internal fun parseBulkPlanGenerationResult(rawJson: String): BulkPlanGenerationResult {
    val root = JSONObject(rawJson)
    val audienceJson = root.optJSONObject("audience") ?: JSONObject()
    val audienceStudentsJson = audienceJson.optJSONArray("students") ?: JSONArray()
    val audienceStudents = buildList {
        for (index in 0 until audienceStudentsJson.length()) {
            val item = audienceStudentsJson.getJSONObject(index)
            add(
                BulkPlanAudienceStudent(
                    studentId = item.getString("student_id"),
                    sources = item.optJSONArray("sources").toSourceLabels()
                )
            )
        }
    }
    val summaryJson = root.optJSONObject("summary") ?: JSONObject()
    val resultsJson = root.optJSONArray("results") ?: JSONArray()
    val results = buildList {
        for (index in 0 until resultsJson.length()) {
            val item = resultsJson.getJSONObject(index)
            add(
                BulkPlanItemResult(
                    studentId = item.getString("student_id"),
                    status = BulkPlanStatus.fromApi(item.optString("status")),
                    plan = item.optJSONObject("plan")?.let(::parseDailyPlanSummary),
                    error = item.optString("error").takeIf { it.isNotBlank() },
                    sources = item.optJSONArray("sources").toSourceLabels()
                )
            )
        }
    }
    return BulkPlanGenerationResult(
        audience = BulkPlanAudience(
            studentIds = audienceJson.optJSONArray("student_ids").toStringList(),
            students = audienceStudents
        ),
        summary = BulkPlanSummary(
            total = summaryJson.optInt("total", results.size),
            generated = summaryJson.optInt("generated", 0),
            existing = summaryJson.optInt("existing", 0),
            failed = summaryJson.optInt("failed", 0)
        ),
        results = results
    )
}

private fun Collection<String>.toJsonArray(): JSONArray = JSONArray().also { array ->
    forEach { array.put(it) }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}

private fun JSONArray?.toSourceLabels(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            when (val source = opt(index)) {
                is JSONObject -> when (source.optString("type")) {
                    "direct" -> add("direct")
                    "group" -> source.optString("group_id")
                        .takeIf { it.isNotBlank() }
                        ?.let { add("group:$it") }
                }

                is String -> source.takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }
}
