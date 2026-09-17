package co.edu.uniautonoma.inclusivereadingar.data.remote

import org.json.JSONObject
import java.time.Instant

interface ProgressApi {
    suspend fun createProgress(
        userId: String,
        action: String,
        learningUnitId: String?,
        device: String,
        payload: Map<String, Any?>,
        accessToken: String?
    )
}

class HttpProgressApi(
    private val httpClient: BackendHttpClient
) : ProgressApi {
    override suspend fun createProgress(
        userId: String,
        action: String,
        learningUnitId: String?,
        device: String,
        payload: Map<String, Any?>,
        accessToken: String?
    ) {
        val body = JSONObject()
            .put("user_id", userId)
            .put("action", action)
            .put("ts", Instant.now().toString())
            .put("device", device)
            .put("payload", JSONObject(payload.filterValues { it != null }))

        learningUnitId?.let { body.put("learning_unit_id", it) }

        httpClient.post(
            path = "/progress",
            body = body,
            accessToken = accessToken
        )
    }
}
