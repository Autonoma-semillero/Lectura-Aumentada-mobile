package co.edu.uniautonoma.inclusivereadingar.data.remote

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

class LiveDomanApiTest {
    @Test
    fun parseLiveSessionExtractsTheActiveCardsAudioUrlSoItCanAutoplay() {
        val json = JSONObject(
            """
            {
              "id": "session-1",
              "group_id": "group-1",
              "repertoire_id": "rep-1",
              "status": "running",
              "presentation_sequence": 3,
              "active_card_index": 1,
              "repertoire_snapshot": {
                "name": "Sala Familia",
                "cards": [
                  {"display_text": "ABUELO", "audio_url": "https://example.com/abuelo.mp3"},
                  {"display_text": "MAMÁ", "audio_url": "https://example.com/mama.mp3"}
                ]
              },
              "presentation": {"display_ms": 1000}
            }
            """.trimIndent()
        )

        val session = parseLiveSession(json)

        assertThat(session.displayText).isEqualTo("MAMÁ")
        assertThat(session.displayAudioUrl).isEqualTo("https://example.com/mama.mp3")
    }

    @Test
    fun parseLiveSessionLeavesAudioUrlNullWhenTheActiveCardHasNone() {
        val json = JSONObject(
            """
            {
              "id": "session-1",
              "group_id": "group-1",
              "repertoire_id": "rep-1",
              "status": "running",
              "active_card_index": 0,
              "repertoire_snapshot": {
                "cards": [{"display_text": "ABUELO"}]
              }
            }
            """.trimIndent()
        )

        val session = parseLiveSession(json)

        assertThat(session.displayAudioUrl).isNull()
    }

    @Test
    fun parseLiveSessionLeavesAudioUrlNullWhenThereIsNoActiveCard() {
        val json = JSONObject(
            """
            {
              "id": "session-1",
              "group_id": "group-1",
              "repertoire_id": "rep-1",
              "status": "lobby",
              "active_card_index": -1,
              "repertoire_snapshot": {"cards": []}
            }
            """.trimIndent()
        )

        val session = parseLiveSession(json)

        assertThat(session.displayAudioUrl).isNull()
    }
}
