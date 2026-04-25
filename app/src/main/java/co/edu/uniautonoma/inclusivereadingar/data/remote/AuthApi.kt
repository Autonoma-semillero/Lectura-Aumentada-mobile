package co.edu.uniautonoma.inclusivereadingar.data.remote

import android.util.Log
import co.edu.uniautonoma.inclusivereadingar.domain.model.AuthSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.SessionUser
import org.json.JSONObject

interface AuthApi {
    suspend fun login(email: String, password: String): AuthSession
}

class HttpAuthApi(
    private val httpClient: BackendHttpClient
) : AuthApi {
    override suspend fun login(email: String, password: String): AuthSession {
        val response = httpClient.post(
            path = "/auth/login",
            body = JSONObject()
                .put("email", email.trim())
                .put("password", password)
        )
        return runCatching {
            parseAuthSession(response)
        }.getOrElse { error ->
            Log.e(TAG, "Invalid login response: $response", error)
            throw BackendException(
                statusCode = 500,
                message = "Respuesta invalida del servidor al iniciar sesion."
            )
        }
    }

    companion object {
        private const val TAG = "AuthApi"

        fun parseAuthSession(rawJson: String): AuthSession {
            val json = JSONObject(rawJson)
            val userJson = json.getJSONObject("user")
            val rolesJson = userJson.optJSONArray("roles")

            return AuthSession(
                accessToken = json.getString("accessToken"),
                user = SessionUser(
                    id = userJson.getString("id"),
                    email = userJson.getString("email"),
                    displayName = userJson.optString("display_name").ifBlank { null },
                    roles = buildList {
                        if (rolesJson != null) {
                            for (index in 0 until rolesJson.length()) {
                                add(rolesJson.getString(index))
                            }
                        }
                    }.ifEmpty { listOf("student") },
                    status = userJson.optString("status").ifBlank { null }
                )
            )
        }
    }
}
