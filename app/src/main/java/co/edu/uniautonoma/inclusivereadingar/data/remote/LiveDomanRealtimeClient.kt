package co.edu.uniautonoma.inclusivereadingar.data.remote

import co.edu.uniautonoma.inclusivereadingar.config.BackendConfig
import co.edu.uniautonoma.inclusivereadingar.data.local.SessionStore
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanParticipant
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSession
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.types.ClientOptions
import io.ably.lib.types.Param
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

interface LiveDomanRealtimeSubscription {
    fun close()
}

internal object NoopLiveDomanRealtimeSubscription : LiveDomanRealtimeSubscription {
    override fun close() = Unit
}

/**
 * Subscribes with a short-lived server-issued token. The Android client only has
 * subscribe permission and cannot publish, choose another room, or see secrets.
 */
class LiveDomanRealtimeClient(private val sessionStore: SessionStore) {
    suspend fun subscribe(
        sessionId: String,
        audience: String,
        onSession: (LiveDomanSession) -> Unit,
        onParticipants: (List<LiveDomanParticipant>) -> Unit,
        onConnection: (Boolean) -> Unit
    ): LiveDomanRealtimeSubscription = withContext(Dispatchers.IO) {
        val options = optionsFor("/doman/live-sessions/$sessionId/realtime-token")
        val realtime = AblyRealtime(options)
        val channel = realtime.channels.get("live-doman:session:$sessionId:$audience")
        channel.subscribe("live.session.updated") { message ->
            runCatching {
                val root = JSONObject(message.data.toString())
                parseLiveSession(root.getJSONObject("session"))
            }.onSuccess(onSession)
        }
        if (audience == "teacher") {
            channel.subscribe("live.participants.updated") { message ->
                runCatching {
                    parseLiveParticipants(JSONObject(message.data.toString()).optJSONArray("participants") ?: org.json.JSONArray())
                }.onSuccess(onParticipants)
            }
        }
        realtime.connection.on(io.ably.lib.realtime.ConnectionEvent.connected) { onConnection(true) }
        realtime.connection.on(io.ably.lib.realtime.ConnectionEvent.disconnected) { onConnection(false) }
        realtime.connection.on(io.ably.lib.realtime.ConnectionEvent.suspended) { onConnection(false) }
        object : LiveDomanRealtimeSubscription {
            override fun close() {
                runCatching { realtime.close() }
            }
        }
    }

    suspend fun subscribeStudentFeed(onRoomOpened: (String) -> Unit, onConnection: (Boolean) -> Unit): LiveDomanRealtimeSubscription = withContext(Dispatchers.IO) {
        val session = requireNotNull(sessionStore.getSession()) { "A valid session is required" }
        val realtime = AblyRealtime(optionsFor("/doman/live-realtime-token"))
        val channel = realtime.channels.get("live-doman:student:${session.user.id}")
        channel.subscribe("live.room.opened") { message ->
            runCatching { JSONObject(message.data.toString()).getString("session_id") }.onSuccess(onRoomOpened)
        }
        realtime.connection.on(io.ably.lib.realtime.ConnectionEvent.connected) { onConnection(true) }
        realtime.connection.on(io.ably.lib.realtime.ConnectionEvent.disconnected) { onConnection(false) }
        realtime.connection.on(io.ably.lib.realtime.ConnectionEvent.suspended) { onConnection(false) }
        object : LiveDomanRealtimeSubscription {
            override fun close() { runCatching { realtime.close() } }
        }
    }

    private suspend fun optionsFor(path: String): ClientOptions {
        val accessToken = requireNotNull(sessionStore.getSession()?.accessToken) { "A valid session is required" }
        val endpoint = BackendConfig.apiUrl(path, sessionStore.resolveBackendBaseUrl())
        return ClientOptions().apply {
            authUrl = endpoint
            authMethod = "POST"
            authHeaders = arrayOf(Param("Authorization", "Bearer $accessToken"))
            useTokenAuth = true
        }
    }
}
