package co.edu.uniautonoma.inclusivereadingar.data.repository

import co.edu.uniautonoma.inclusivereadingar.data.local.SessionStore
import co.edu.uniautonoma.inclusivereadingar.data.remote.ArAssetsApi
import co.edu.uniautonoma.inclusivereadingar.domain.model.ArAsset
import co.edu.uniautonoma.inclusivereadingar.domain.repository.ArAssetRepository

class BackendArAssetRepository(
    private val sessionStore: SessionStore,
    private val assetsApi: ArAssetsApi
) : ArAssetRepository {
    override suspend fun findByMarker(markerId: String): ArAsset? {
        val session = checkNotNull(sessionStore.getSession()) { "An authenticated session is required" }
        val asset = assetsApi.findByMarker(markerId, session.accessToken) ?: return null
        val baseUrl = sessionStore.resolveBackendBaseUrl()
        return asset.copy(
            model3dUrl = asset.model3dUrl?.let { resolveAssetUrl(it, baseUrl) },
            audioUrl = asset.audioUrl?.let { resolveAssetUrl(it, baseUrl) }
        )
    }

    private fun resolveAssetUrl(assetUrl: String, backendBaseUrl: String): String {
        val value = assetUrl.trim()
        val base = backendBaseUrl.removeSuffix("/")
        return when {
            value.startsWith("https://") || value.startsWith("http://") -> value
            value.startsWith("/") -> "$base$value"
            else -> error("Unsupported AR asset URL")
        }
    }
}
