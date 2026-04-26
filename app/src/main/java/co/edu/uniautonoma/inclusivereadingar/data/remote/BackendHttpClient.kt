package co.edu.uniautonoma.inclusivereadingar.data.remote

import android.util.Log
import co.edu.uniautonoma.inclusivereadingar.config.BackendConfig
import co.edu.uniautonoma.inclusivereadingar.data.local.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class BackendHttpClient(
    private val sessionStore: SessionStore
) {

    suspend fun get(
        path: String,
        queryParams: Map<String, String> = emptyMap(),
        accessToken: String? = null
    ): String = request(
        method = "GET",
        path = path,
        queryParams = queryParams,
        body = null,
        accessToken = accessToken
    )

    suspend fun post(
        path: String,
        body: JSONObject,
        accessToken: String? = null
    ): String = request(
        method = "POST",
        path = path,
        queryParams = emptyMap(),
        body = body.toString(),
        accessToken = accessToken
    )

    suspend fun requestRaw(
        method: String,
        path: String,
        body: JSONObject? = null,
        accessToken: String? = null
    ): String = request(
        method = method,
        path = path,
        queryParams = emptyMap(),
        body = body?.toString(),
        accessToken = accessToken
    )

    private suspend fun request(
        method: String,
        path: String,
        queryParams: Map<String, String>,
        body: String?,
        accessToken: String?
    ): String = withContext(Dispatchers.IO) {
        val baseUrl = sessionStore.resolveBackendBaseUrl()
        val fullUrl = buildUrl(baseUrl, path, queryParams)
        Log.d(TAG, "Request: $method $fullUrl")
        if (body != null) {
            Log.d(TAG, "Request body: $body")
        }
        val connection = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (!accessToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            doInput = true
            if (body != null) {
                doOutput = true
            }
        }

        try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
            }

            val statusCode = connection.responseCode
            val responseBody = readResponse(connection, statusCode)
            Log.d(TAG, "Response: $statusCode $responseBody")
            if (statusCode in 200..299) {
                responseBody
            } else {
                throw BackendException(
                    statusCode = statusCode,
                    message = extractBackendMessage(responseBody, connection.responseMessage)
                )
            }
        } catch (error: BackendException) {
            Log.e(TAG, "Backend error on $method $fullUrl", error)
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Transport error on $method $fullUrl", error)
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUrl(baseUrl: String, path: String, queryParams: Map<String, String>): String {
        val endpoint = BackendConfig.apiUrl(path, baseUrl)
        if (queryParams.isEmpty()) {
            return endpoint
        }
        val query = queryParams.entries.joinToString("&") { (key, value) ->
            "${key.encode()}=${value.encode()}"
        }
        return "$endpoint?$query"
    }

    private fun String.encode(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8.name())

    private fun readResponse(connection: HttpURLConnection, statusCode: Int): String {
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        if (stream == null) {
            return ""
        }
        return BufferedReader(stream.reader(Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }

    private fun extractBackendMessage(body: String, fallback: String): String {
        return runCatching {
            val json = JSONObject(body)
            when {
                json.has("message") -> json.get("message").toString()
                json.has("error") -> json.get("error").toString()
                else -> fallback
            }
        }.getOrDefault(body.ifBlank { fallback })
    }

    private companion object {
        private const val TIMEOUT_MS = 15_000
        private const val TAG = "BackendHttpClient"
    }
}
