package co.edu.uniautonoma.inclusivereadingar.data.remote

import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanCard
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanParticipant
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanRepertoire
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSessionDetail
import org.json.JSONArray
import org.json.JSONObject

/** HTTP boundary for the teacher-led Doman room. No local timer advances a card. */
interface LiveDomanApi {
    suspend fun getCards(accessToken: String?): List<LiveDomanCard>
    suspend fun createCard(word: String, categoryId: String, accessToken: String?): LiveDomanCard
    suspend fun getRepertoires(accessToken: String?): List<LiveDomanRepertoire>
    suspend fun createRepertoire(name: String, categoryId: String, cardIds: List<String>, accessToken: String?): LiveDomanRepertoire
    suspend fun createSession(groupId: String, repertoireId: String, accessToken: String?): LiveDomanSession
    suspend fun getCurrent(accessToken: String?): List<LiveDomanSession>
    suspend fun listSessions(accessToken: String?): List<LiveDomanSession>
    suspend fun getSession(sessionId: String, accessToken: String?): LiveDomanSessionDetail
    suspend fun join(sessionId: String, accessToken: String?): LiveDomanSession
    suspend fun heartbeat(sessionId: String, accessToken: String?)
    suspend fun command(
        sessionId: String,
        action: String,
        expectedSequence: Int,
        commandId: String,
        accessToken: String?
    ): LiveDomanSession
    suspend fun acknowledge(sessionId: String, sequence: Int, accessToken: String?)
}

class HttpLiveDomanApi(private val httpClient: BackendHttpClient) : LiveDomanApi {
    override suspend fun getCards(accessToken: String?): List<LiveDomanCard> =
        parseCards(httpClient.get("/doman/cards", accessToken = accessToken))

    override suspend fun createCard(word: String, categoryId: String, accessToken: String?): LiveDomanCard {
        val root = JSONObject(httpClient.post("/doman/cards", JSONObject().put("word", word).put("category_id", categoryId), accessToken))
        return parseCards(JSONArray().put(root).toString()).single()
    }

    override suspend fun getRepertoires(accessToken: String?): List<LiveDomanRepertoire> =
        parseRepertoires(httpClient.get("/doman/repertoires", accessToken = accessToken))

    override suspend fun createRepertoire(name: String, categoryId: String, cardIds: List<String>, accessToken: String?): LiveDomanRepertoire {
        val root = JSONObject(httpClient.post(
            "/doman/repertoires",
            JSONObject().put("name", name).put("category_id", categoryId).put("stage", "single_words").put("card_ids", JSONArray(cardIds)),
            accessToken
        ))
        return parseRepertoires(JSONArray().put(root).toString()).single()
    }

    override suspend fun createSession(
        groupId: String,
        repertoireId: String,
        accessToken: String?
    ): LiveDomanSession = parseSession(
        JSONObject(
            httpClient.post(
                "/doman/live-sessions",
                JSONObject().put("group_id", groupId).put("repertoire_id", repertoireId),
                accessToken
            )
        )
    )

    override suspend fun getCurrent(accessToken: String?): List<LiveDomanSession> {
        val raw = httpClient.get("/doman/live-sessions/current", accessToken = accessToken)
        val array = if (raw.trimStart().startsWith("[")) {
            JSONArray(raw)
        } else {
            JSONObject(raw).optJSONArray("sessions") ?: JSONArray()
        }
        return buildList { for (index in 0 until array.length()) add(parseSession(array.getJSONObject(index))) }
    }

    override suspend fun listSessions(accessToken: String?): List<LiveDomanSession> {
        val raw = httpClient.get("/doman/live-sessions", accessToken = accessToken)
        val array = if (raw.trimStart().startsWith("[")) {
            JSONArray(raw)
        } else {
            JSONObject(raw).optJSONArray("sessions") ?: JSONArray()
        }
        return buildList { for (index in 0 until array.length()) add(parseLiveSession(array.getJSONObject(index))) }
    }

    override suspend fun getSession(sessionId: String, accessToken: String?): LiveDomanSessionDetail {
        val root = JSONObject(httpClient.get("/doman/live-sessions/$sessionId", accessToken = accessToken))
        val session = parseSession(root.optJSONObject("session") ?: root)
        val participants = root.optJSONArray("participants") ?: JSONArray()
        return LiveDomanSessionDetail(
            session = session,
            participants = parseLiveParticipants(participants)
        )
    }

    override suspend fun join(sessionId: String, accessToken: String?): LiveDomanSession {
        val root = JSONObject(httpClient.post("/doman/live-sessions/$sessionId/join", JSONObject(), accessToken))
        return parseSession(root.optJSONObject("session") ?: root)
    }

    override suspend fun heartbeat(sessionId: String, accessToken: String?) {
        httpClient.post("/doman/live-sessions/$sessionId/heartbeat", JSONObject(), accessToken)
    }

    override suspend fun command(
        sessionId: String,
        action: String,
        expectedSequence: Int,
        commandId: String,
        accessToken: String?
    ): LiveDomanSession {
        val root = JSONObject(
            httpClient.post(
                "/doman/live-sessions/$sessionId/commands",
                JSONObject()
                    .put("action", action)
                    .put("expected_sequence", expectedSequence)
                    .put("command_id", commandId),
                accessToken
            )
        )
        return parseSession(root.optJSONObject("session") ?: root)
    }

    override suspend fun acknowledge(sessionId: String, sequence: Int, accessToken: String?) {
        httpClient.post(
            "/doman/live-sessions/$sessionId/acknowledge",
            JSONObject().put("sequence", sequence),
            accessToken
        )
    }

    private fun parseCards(raw: String): List<LiveDomanCard> {
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    LiveDomanCard(
                        id = item.getString("id"),
                        text = item.optString("display_text").ifBlank { item.optString("word") },
                        categoryId = item.optString("category_id").ifBlank { null },
                        status = item.optString("status", "active")
                    )
                )
            }
        }
    }

    private fun parseRepertoires(raw: String): List<LiveDomanRepertoire> {
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val cards = item.optJSONArray("cards") ?: item.optJSONArray("card_ids")
                add(
                    LiveDomanRepertoire(
                        id = item.getString("id"),
                        name = item.optString("name").ifBlank { item.optString("title") },
                        description = item.optString("description").ifBlank { null },
                        cardCount = item.optInt("card_count", cards?.length() ?: 0),
                        status = item.optString("status", "active"),
                        categoryId = item.optString("category_id").ifBlank { null }
                    )
                )
            }
        }
    }

    internal fun parseSession(item: JSONObject): LiveDomanSession = parseLiveSession(item)
}

internal fun parseLiveParticipants(participants: JSONArray): List<LiveDomanParticipant> = buildList {
    for (index in 0 until participants.length()) {
        val item = participants.getJSONObject(index)
        add(
            LiveDomanParticipant(
                studentId = item.optString("student_id"),
                status = item.optString("connection_state", item.optString("status", "invited")),
                acknowledgedSequence = item.optInt("current_ack_sequence", item.optInt("acknowledged_sequence", -1))
            )
        )
    }
}

internal fun parseLiveSession(item: JSONObject): LiveDomanSession {
        val repertoire = item.optJSONObject("repertoire_snapshot")
        val activeCardIndex = item.optInt("active_card_index", -1)
        val card = repertoire?.optJSONArray("cards")
            ?.takeIf { activeCardIndex in 0 until it.length() }
            ?.optJSONObject(activeCardIndex)
        val presentation = item.optJSONObject("presentation")
        return LiveDomanSession(
            id = item.getString("id"),
            groupId = item.optString("group_id"),
            repertoireId = item.optString("repertoire_id"),
            repertoireName = repertoire?.optString("name")?.ifBlank { null }
                ?: item.optString("repertoire_name").ifBlank { null },
            status = item.optString("status", "lobby"),
            sequence = item.optInt("presentation_sequence", item.optInt("sequence", 0)),
            displayText = card?.optString("display_text")?.ifBlank { null }
                ?: item.optString("display_text").ifBlank { null },
            displayAudioUrl = card?.optString("audio_url")?.ifBlank { null },
            displayDurationMs = presentation?.optInt("display_ms", 0)
                ?: item.optInt("display_duration_ms", item.optInt("display_ms", 0))
        )
}
