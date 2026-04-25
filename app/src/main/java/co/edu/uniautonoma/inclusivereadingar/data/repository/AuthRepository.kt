package co.edu.uniautonoma.inclusivereadingar.data.repository

import co.edu.uniautonoma.inclusivereadingar.data.local.SessionStore
import co.edu.uniautonoma.inclusivereadingar.data.remote.AuthApi
import co.edu.uniautonoma.inclusivereadingar.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

interface AuthSessionRepository {
    val sessionFlow: Flow<AuthSession?>
    suspend fun login(email: String, password: String): AuthSession
    suspend fun logout()
    suspend fun currentSession(): AuthSession?
}

class AuthRepository(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore
) : AuthSessionRepository {
    override val sessionFlow: Flow<AuthSession?> = sessionStore.sessionFlow

    override suspend fun login(email: String, password: String): AuthSession {
        val session = authApi.login(email, password)
        sessionStore.saveSession(session)
        return session
    }

    override suspend fun logout() {
        sessionStore.clearSession()
    }

    override suspend fun currentSession(): AuthSession? = sessionStore.getSession()
}
