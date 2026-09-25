package co.edu.uniautonoma.inclusivereadingar.data.repository

import co.edu.uniautonoma.inclusivereadingar.data.local.SessionStore
import co.edu.uniautonoma.inclusivereadingar.data.remote.LiveDomanApi
import co.edu.uniautonoma.inclusivereadingar.domain.model.AuthSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanRepertoire
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSessionDetail

interface LiveDomanDataSource {
    suspend fun getRepertoires(): List<LiveDomanRepertoire>
    suspend fun createSession(groupId: String, repertoireId: String): LiveDomanSession
    suspend fun getCurrent(): List<LiveDomanSession>
    suspend fun getSession(sessionId: String): LiveDomanSessionDetail
    suspend fun join(sessionId: String): LiveDomanSession
    suspend fun command(sessionId: String, action: String, expectedSequence: Int, commandId: String): LiveDomanSession
    suspend fun acknowledge(sessionId: String, sequence: Int)
}

class LiveDomanRepository(
    private val sessionStore: SessionStore,
    private val api: LiveDomanApi
) : LiveDomanDataSource {
    override suspend fun getRepertoires() = api.getRepertoires(requireSession().accessToken)
    override suspend fun createSession(groupId: String, repertoireId: String) =
        api.createSession(groupId, repertoireId, requireSession().accessToken)
    override suspend fun getCurrent() = api.getCurrent(requireSession().accessToken)
    override suspend fun getSession(sessionId: String) = api.getSession(sessionId, requireSession().accessToken)
    override suspend fun join(sessionId: String) = api.join(sessionId, requireSession().accessToken)
    override suspend fun command(sessionId: String, action: String, expectedSequence: Int, commandId: String) =
        api.command(sessionId, action, expectedSequence, commandId, requireSession().accessToken)
    override suspend fun acknowledge(sessionId: String, sequence: Int) =
        api.acknowledge(sessionId, sequence, requireSession().accessToken)

    private suspend fun requireSession(): AuthSession = requireNotNull(sessionStore.getSession()) {
        "A valid session is required"
    }
}
