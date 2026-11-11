package co.edu.uniautonoma.inclusivereadingar.data.remote

import co.edu.uniautonoma.inclusivereadingar.domain.model.WordCard
import org.json.JSONArray
import org.json.JSONObject

/**
 * Helpers for the reusable card catalog (`/doman/cards`). Kept free of Android and HTTP types
 * so they can be unit tested; the endpoint has no category filter and no get-by-id, so those
 * are resolved here on the client.
 */
internal fun parseCatalogCard(rawJson: String): WordCard {
    val item = JSONObject(rawJson)
    val word = item.getString("word")
    return WordCard(
        id = item.getString("id"),
        word = word,
        status = item.getString("status"),
        categoryId = item.optString("category_id").ifBlank { null },
        learningUnitId = item.optString("learning_unit_id").ifBlank { null },
        audioUrl = item.optString("audio_url").ifBlank { null },
        language = item.optString("language").ifBlank { null },
        initialLetter = item.optString("initial_letter").ifBlank { word.take(1).uppercase() },
        timesShown = item.optInt("times_shown", 0),
        timesAudioPlayed = item.optInt("times_audio_played", -1).takeIf { it >= 0 }
    )
}

internal fun parseCatalogCards(rawJson: String): List<WordCard> {
    val json = JSONArray(rawJson)
    return buildList {
        for (index in 0 until json.length()) {
            add(parseCatalogCard(json.getJSONObject(index).toString()))
        }
    }
}

internal fun List<WordCard>.inCategory(categoryId: String): List<WordCard> =
    filter { it.categoryId == categoryId }

internal fun List<WordCard>.requireById(id: String): WordCard =
    firstOrNull { it.id == id } ?: throw IllegalStateException("No se encontró la tarjeta.")
