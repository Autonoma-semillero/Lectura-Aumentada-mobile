package co.edu.uniautonoma.inclusivereadingar.config

import co.edu.uniautonoma.inclusivereadingar.BuildConfig

object BackendConfig {
    const val API_PREFIX: String = "/api"
    private const val DEFAULT_REMOTE_BASE_URL: String = BuildConfig.BACKEND_BASE_URL

    fun resolveBaseUrl(overrideUrl: String?): String {
        val candidate = overrideUrl?.trim().orEmpty().ifEmpty { DEFAULT_REMOTE_BASE_URL }
        return candidate.removeSuffix("/")
    }

    fun apiUrl(path: String, overrideUrl: String? = null): String {
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return "${resolveBaseUrl(overrideUrl)}$API_PREFIX$normalizedPath"
    }
}
