package co.edu.uniautonoma.inclusivereadingar.data.remote

import android.net.Uri
import co.edu.uniautonoma.inclusivereadingar.domain.model.ArAsset
import co.edu.uniautonoma.inclusivereadingar.domain.ocr.OcrWordMatchPolicy
import co.edu.uniautonoma.inclusivereadingar.domain.ocr.OcrWordNormalizer
import org.json.JSONObject

interface ArAssetsApi {
    suspend fun findByMarker(markerId: String, accessToken: String): ArAsset?
    suspend fun findByWord(word: String, accessToken: String): ArAsset?
}

class HttpArAssetsApi(
    private val httpClient: BackendHttpClient
) : ArAssetsApi {
    override suspend fun findByMarker(markerId: String, accessToken: String): ArAsset? {
        val normalizedMarkerId = markerId.trim()
        require(normalizedMarkerId.matches(MARKER_ID_PATTERN)) { "Invalid marker identifier" }

        val asset = requestAsset(
            path = "/assets/marker/${Uri.encode(normalizedMarkerId)}",
            accessToken = accessToken
        ) ?: return null
        require(asset.markerId == normalizedMarkerId) {
            "Marker response does not match the request"
        }
        return asset
    }

    override suspend fun findByWord(word: String, accessToken: String): ArAsset? {
        val normalizedWord = requireNotNull(OcrWordNormalizer.normalize(word)) {
            "Invalid OCR word"
        }
        val asset = requestAsset(
            path = "/assets/word/${Uri.encode(normalizedWord)}",
            accessToken = accessToken
        ) ?: return null
        require(OcrWordMatchPolicy.acceptsAssetResponse(normalizedWord, asset.word)) {
            "Word response does not match the OCR request"
        }
        return asset
    }

    private suspend fun requestAsset(path: String, accessToken: String): ArAsset? {
        val response = try {
            httpClient.get(path = path, accessToken = accessToken)
        } catch (error: BackendException) {
            if (error.statusCode == 404) return null
            throw error
        }

        if (response.isBlank() || response.trim() == "null") return null
        return parseAsset(JSONObject(response))
    }

    private fun parseAsset(json: JSONObject): ArAsset {
        val id = json.requiredString("id")
        val learningUnitId = json.requiredString("learning_unit_id")
        val markerId = json.requiredString("marker_id")
        val word = json.requiredString("word")

        val accessibility = json.optJSONObject("metadata_accessibility")
        val accessibilityLabel = accessibility?.firstNonBlank(
            "description",
            "alt_text",
            "label"
        )

        return ArAsset(
            id = id,
            learningUnitId = learningUnitId,
            markerId = markerId,
            word = word,
            model3dUrl = json.optionalAssetUrl("model_3d"),
            audioUrl = json.optionalAssetUrl("audio_pronunciacion"),
            accessibilityLabel = accessibilityLabel
        )
    }

    private fun JSONObject.requiredString(key: String): String {
        val value = optString(key).trim()
        require(value.isNotEmpty()) { "Missing or invalid $key in AR asset response" }
        return value
    }

    private fun JSONObject.optionalAssetUrl(key: String): String? {
        val value = optString(key).trim().takeIf(String::isNotEmpty) ?: return null
        val accepted = value.startsWith("https://") ||
            value.startsWith("http://") ||
            value.startsWith("/")
        require(accepted) { "Unsupported URL in $key" }
        return value
    }

    private fun JSONObject.firstNonBlank(vararg keys: String): String? = keys
        .asSequence()
        .map(::optString)
        .map(String::trim)
        .firstOrNull(String::isNotEmpty)

    private companion object {
        val MARKER_ID_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$")
    }
}
