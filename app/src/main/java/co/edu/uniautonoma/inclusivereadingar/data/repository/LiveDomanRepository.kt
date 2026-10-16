package co.edu.uniautonoma.inclusivereadingar.data.repository

import co.edu.uniautonoma.inclusivereadingar.data.local.SessionStore
import co.edu.uniautonoma.inclusivereadingar.data.remote.LiveDomanApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.LiveDomanRealtimeClient
import co.edu.uniautonoma.inclusivereadingar.data.remote.LiveDomanRealtimeSubscription
import co.edu.uniautonoma.inclusivereadingar.data.remote.NoopLiveDomanRealtimeSubscription
import co.edu.uniautonoma.inclusivereadingar.domain.model.AuthSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanRepertoire
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSessionDetail

interface LiveDomanDataSource {
    suspend fun getRepertoires(): List<LiveDomanRepertoire>
    /** Defaults keep existing fake data sources source-compatible while Live setup adopts content authoring. */
    suspend fun getCards(): List<co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanCard> = emptyList()
    suspend fun createCard(word: String, categoryId: String): co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanCard =
        error("La creación de tarjetas Live Doman no está disponible.")
    suspend fun createRepertoire(name: String, categoryId: String, cardIds: List<String>): LiveDomanRepertoire =
        error("La creación de repertorios Live Doman no está disponible.")
    suspend fun createSession(groupId: String, repertoireId: String): LiveDomanSession
    suspend fun getCurrent(): List<LiveDomanSession>
    suspend fun getSession(sessionId: String): LiveDomanSessionDetail
    suspend fun join(sessionId: String): LiveDomanSession
    suspend fun heartbeat(sessionId: String) = Unit
    suspend fun command(sessionId: String, action: String, expectedSequence: Int, commandId: String): LiveDomanSession
    suspend fun acknowledge(sessionId: String, sequence: Int)
    suspend fun subscribe(
        sessionId: String,
        audience: String,
        onSession: (LiveDomanSession) -> Unit,
        onParticipants: (List<co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanParticipant>) -> Unit = {},
        onConnection: (Boolean) -> Unit = {}
    ): LiveDomanRealtimeSubscription = NoopLiveDomanRealtimeSubscription
    suspend fun subscribeStudentFeed(
        onRoomOpened: (String) -> Unit,
        onConnection: (Boolean) -> Unit = {}
    ): LiveDomanRealtimeSubscription = NoopLiveDomanRealtimeSubscription
}

class LiveDomanRepository(
    private val sessionStore: SessionStore,
    private val api: LiveDomanApi,
    private val realtime: LiveDomanRealtimeClient = LiveDomanRealtimeClient(sessionStore)
) : LiveDomanDataSource {
    override suspend fun getRepertoires() = api.getRepertoires(requireSession().accessToken)
    override suspend fun getCards() = api.getCards(requireSession().accessToken)
    override suspend fun createCard(word: String, categoryId: String) = api.createCard(word, categoryId, requireSession().accessToken)
    override suspend fun createRepertoire(name: String, categoryId: String, cardIds: List<String>) =
        api.createRepertoire(name, categoryId, cardIds, requireSession().accessToken)
    override suspend fun createSession(groupId: String, repertoireId: String) =
        api.createSession(groupId, repertoireId, requireSession().accessToken)
    override suspend fun getCurrent() = api.getCurrent(requireSession().accessToken)
    override suspend fun getSession(sessionId: String) = api.getSession(sessionId, requireSession().accessToken)
    override suspend fun join(sessionId: String) = api.join(sessionId, requireSession().accessToken)
    override suspend fun heartbeat(sessionId: String) = api.heartbeat(sessionId, requireSession().accessToken)
    override suspend fun command(sessionId: String, action: String, expectedSequence: Int, commandId: String) =
        api.command(sessionId, action, expectedSequence, commandId, requireSession().accessToken)
    override suspend fun acknowledge(sessionId: String, sequence: Int) =
        api.acknowledge(sessionId, sequence, requireSession().accessToken)
    override suspend fun subscribe(
        sessionId: String,
        audience: String,
        onSession: (LiveDomanSession) -> Unit,
        onParticipants: (List<co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanParticipant>) -> Unit,
        onConnection: (Boolean) -> Unit
    ) = realtime.subscribe(sessionId, audience, onSession, onParticipants, onConnection)
    override suspend fun subscribeStudentFeed(onRoomOpened: (String) -> Unit, onConnection: (Boolean) -> Unit) =
        realtime.subscribeStudentFeed(onRoomOpened, onConnection)

    private suspend fun requireSession(): AuthSession = requireNotNull(sessionStore.getSession()) {
        "A valid session is required"
    }
}
