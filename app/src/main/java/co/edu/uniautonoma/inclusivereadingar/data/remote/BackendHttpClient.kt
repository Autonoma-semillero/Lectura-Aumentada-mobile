package co.edu.uniautonoma.inclusivereadingar.data.remote

import android.util.Log
import co.edu.uniautonoma.inclusivereadingar.config.BackendConfig
import co.edu.uniautonoma.inclusivereadingar.data.local.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

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

    suspend fun postMultipartFile(
        path: String,
        fieldName: String,
        filePath: String,
        mimeType: String?,
        originalName: String?,
        accessToken: String? = null
    ): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        require(file.exists() && file.isFile) {
            "Selected audio file does not exist: $filePath"
        }

        val baseUrl = sessionStore.resolveBackendBaseUrl()
        val fullUrl = buildUrl(baseUrl, path, emptyMap())
        val boundary = "Boundary-${UUID.randomUUID()}"

        Log.d(TAG, "Request: POST $fullUrl (multipart)")

        val connection = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doInput = true
            doOutput = true
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            if (!accessToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
        }

        try {
            DataOutputStream(connection.outputStream).use { output ->
                val lineEnd = "\r\n"
                val twoHyphens = "--"
                val filename = sanitizeFileName(originalName ?: file.name)
                val resolvedMimeType = mimeType?.takeIf { it.isNotBlank() } ?: guessMimeType(filename)

                output.writeBytes("$twoHyphens$boundary$lineEnd")
                output.writeBytes(
                    "Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$filename\"$lineEnd"
                )
                output.writeBytes("Content-Type: $resolvedMimeType$lineEnd")
                output.writeBytes(lineEnd)
                file.inputStream().use { input -> input.copyTo(output) }
                output.writeBytes(lineEnd)
                output.writeBytes("$twoHyphens$boundary$twoHyphens$lineEnd")
                output.flush()
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
            Log.e(TAG, "Backend error on POST multipart $fullUrl", error)
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Transport error on POST multipart $fullUrl", error)
            throw error
        } finally {
            connection.disconnect()
        }
    }

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

    private fun sanitizeFileName(filename: String): String =
        filename.replace("\"", "").replace("\n", "").replace("\r", "").trim()
            .ifBlank { "audio.m4a" }

    private fun guessMimeType(filename: String): String {
        val lower = filename.lowercase()
        return when {
            lower.endsWith(".mp3") -> "audio/mpeg"
            lower.endsWith(".wav") -> "audio/wav"
            else -> "audio/mp4"
        }
    }

    private companion object {
        private const val TIMEOUT_MS = 15_000
        private const val TAG = "BackendHttpClient"
    }
}
