package co.edu.uniautonoma.inclusivereadingar.config

object WebArConfig {
    const val USE_MOCK_DATA: Boolean = true
    const val API_BASE_URL: String = "https://api.example.edu/api/v1"

    // Local fallback activity so the app works without deployed web content.
    const val WEB_AR_URL: String = "file:///android_asset/reading_activity.html"

    const val ALLOW_MIXED_CONTENT: Boolean = false
}
