package co.edu.uniautonoma.inclusivereadingar.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import co.edu.uniautonoma.inclusivereadingar.config.BackendConfig
import co.edu.uniautonoma.inclusivereadingar.domain.model.AuthSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.SessionUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.sessionDataStore by preferencesDataStore(name = "student_session")

class SessionStore(private val context: Context) {

    val sessionFlow: Flow<AuthSession?> = context.sessionDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map(::toAuthSession)

    suspend fun getSession(): AuthSession? = sessionFlow.first()

    suspend fun saveSession(session: AuthSession) {
        context.sessionDataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = session.accessToken
            preferences[USER_ID] = session.user.id
            preferences[USER_EMAIL] = session.user.email
            preferences[USER_DISPLAY_NAME] = session.user.displayName.orEmpty()
            preferences[USER_ROLES] = session.user.roles.joinToString(",")
            preferences[USER_STATUS] = session.user.status.orEmpty()
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(USER_ID)
            preferences.remove(USER_EMAIL)
            preferences.remove(USER_DISPLAY_NAME)
            preferences.remove(USER_ROLES)
            preferences.remove(USER_STATUS)
        }
    }

    suspend fun resolveBackendBaseUrl(): String {
        val preferences = context.sessionDataStore.data.first()
        return BackendConfig.resolveBaseUrl(preferences[BACKEND_BASE_URL_OVERRIDE])
    }

    suspend fun saveBackendBaseUrlOverride(overrideUrl: String?) {
        context.sessionDataStore.edit { preferences ->
            if (overrideUrl.isNullOrBlank()) {
                preferences.remove(BACKEND_BASE_URL_OVERRIDE)
            } else {
                preferences[BACKEND_BASE_URL_OVERRIDE] = overrideUrl.trim()
            }
        }
    }

    private fun toAuthSession(preferences: Preferences): AuthSession? {
        val accessToken = preferences[ACCESS_TOKEN] ?: return null
        val userId = preferences[USER_ID] ?: return null
        val email = preferences[USER_EMAIL] ?: return null
        val displayName = preferences[USER_DISPLAY_NAME].orEmpty().ifBlank { null }
        val roles = preferences[USER_ROLES]
            .orEmpty()
            .split(',')
            .map(String::trim)
            .filter(String::isNotBlank)
            .ifEmpty { listOf("student") }
        val status = preferences[USER_STATUS].orEmpty().ifBlank { null }

        return AuthSession(
            accessToken = accessToken,
            user = SessionUser(
                id = userId,
                email = email,
                displayName = displayName,
                roles = roles,
                status = status
            )
        )
    }

    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        private val USER_ROLES = stringPreferencesKey("user_roles")
        private val USER_STATUS = stringPreferencesKey("user_status")
        private val BACKEND_BASE_URL_OVERRIDE = stringPreferencesKey("backend_base_url_override")
    }
}
