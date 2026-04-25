package co.edu.uniautonoma.inclusivereadingar.data.remote

import co.edu.uniautonoma.inclusivereadingar.domain.model.WordCard
import org.json.JSONArray

interface WordCardsApi {
    suspend fun getWordCards(
        studentId: String,
        categoryId: String,
        accessToken: String?
    ): List<WordCard>
}

class HttpWordCardsApi(
    private val httpClient: BackendHttpClient
) : WordCardsApi {
    override suspend fun getWordCards(
        studentId: String,
        categoryId: String,
        accessToken: String?
    ): List<WordCard> {
        val response = httpClient.get(
            path = "/word-cards",
            queryParams = mapOf(
                "student_id" to studentId,
                "category_id" to categoryId
            ),
            accessToken = accessToken
        )
        return parseWordCards(response)
    }

    private fun parseWordCards(rawJson: String): List<WordCard> {
        val json = JSONArray(rawJson)
        return buildList {
            for (index in 0 until json.length()) {
                val item = json.getJSONObject(index)
                add(
                    WordCard(
                        id = item.getString("id"),
                        studentId = item.getString("student_id"),
                        word = item.getString("word"),
                        status = item.getString("status"),
                        categoryId = item.optString("category_id").ifBlank { null },
                        learningUnitId = item.optString("learning_unit_id").ifBlank { null },
                        audioUrl = item.optString("audio_url").ifBlank { null },
                        language = item.optString("language").ifBlank { null },
                        initialLetter = item.optString("initial_letter"),
                        timesShown = item.optInt("times_shown", 0),
                        timesAudioPlayed = item.optInt("times_audio_played", -1)
                            .takeIf { it >= 0 }
                    )
                )
            }
        }
    }
}
