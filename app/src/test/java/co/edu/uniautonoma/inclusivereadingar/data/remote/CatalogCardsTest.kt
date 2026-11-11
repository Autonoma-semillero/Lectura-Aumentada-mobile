package co.edu.uniautonoma.inclusivereadingar.data.remote

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogCardsTest {
    @Test
    fun parseCatalogCard_fullPayload_mapsEveryField() {
        val card = parseCatalogCard(
            """
            {
              "id": "card-1",
              "word": "gato",
              "status": "active",
              "category_id": "cat-1",
              "learning_unit_id": "lu-1",
              "audio_url": "https://cdn.test/gato.mp3",
              "language": "es-CO"
            }
            """.trimIndent()
        )

        assertThat(card.id).isEqualTo("card-1")
        assertThat(card.word).isEqualTo("gato")
        assertThat(card.status).isEqualTo("active")
        assertThat(card.categoryId).isEqualTo("cat-1")
        assertThat(card.learningUnitId).isEqualTo("lu-1")
        assertThat(card.audioUrl).isEqualTo("https://cdn.test/gato.mp3")
        assertThat(card.language).isEqualTo("es-CO")
    }

    @Test
    fun parseCatalogCard_catalogCardHasNoStudentAndNoProgress() {
        val card = parseCatalogCard("""{"id": "c", "word": "perro", "status": "active"}""")

        assertThat(card.studentId).isNull()
        assertThat(card.timesShown).isEqualTo(0)
        assertThat(card.timesAudioPlayed).isNull()
    }

    @Test
    fun parseCatalogCard_missingOptionalFields_becomeNull() {
        val card = parseCatalogCard(
            """{"id": "c", "word": "perro", "status": "active", "audio_url": "", "learning_unit_id": ""}"""
        )

        assertThat(card.categoryId).isNull()
        assertThat(card.learningUnitId).isNull()
        assertThat(card.audioUrl).isNull()
        assertThat(card.language).isNull()
    }

    @Test
    fun parseCatalogCard_initialLetterFallsBackToTheUppercasedFirstLetter() {
        val derived = parseCatalogCard("""{"id": "c", "word": "elefante", "status": "active"}""")
        val provided = parseCatalogCard(
            """{"id": "c", "word": "elefante", "status": "active", "initial_letter": "E"}"""
        )

        assertThat(derived.initialLetter).isEqualTo("E")
        assertThat(provided.initialLetter).isEqualTo("E")
    }

    @Test
    fun parseCatalogCard_keepsLegacyProgressFieldsWhenPresent() {
        val card = parseCatalogCard(
            """{"id": "c", "word": "pato", "status": "active", "times_shown": 4, "times_audio_played": 2}"""
        )

        assertThat(card.timesShown).isEqualTo(4)
        assertThat(card.timesAudioPlayed).isEqualTo(2)
    }

    @Test
    fun parseCatalogCards_parsesEveryItemInOrder() {
        val cards = parseCatalogCards(
            """
            [
              {"id": "1", "word": "gato", "status": "active", "category_id": "a"},
              {"id": "2", "word": "pato", "status": "active", "category_id": "b"}
            ]
            """.trimIndent()
        )

        assertThat(cards.map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun parseCatalogCards_emptyArray_returnsEmptyList() {
        assertThat(parseCatalogCards("[]")).isEmpty()
    }

    @Test
    fun inCategory_keepsOnlyCardsOfThatCategory() {
        val cards = parseCatalogCards(
            """
            [
              {"id": "1", "word": "gato", "status": "active", "category_id": "animales"},
              {"id": "2", "word": "rojo", "status": "active", "category_id": "colores"},
              {"id": "3", "word": "pato", "status": "active", "category_id": "animales"},
              {"id": "4", "word": "sin", "status": "active"}
            ]
            """.trimIndent()
        )

        assertThat(cards.inCategory("animales").map { it.id }).containsExactly("1", "3").inOrder()
        assertThat(cards.inCategory("nada")).isEmpty()
    }

    @Test
    fun requireById_returnsTheMatchingCard() {
        val cards = parseCatalogCards(
            """[{"id": "1", "word": "gato", "status": "active"}, {"id": "2", "word": "pato", "status": "active"}]"""
        )

        assertThat(cards.requireById("2").word).isEqualTo("pato")
    }

    @Test
    fun requireById_unknownId_throwsWithAReadableMessage() {
        val cards = parseCatalogCards("""[{"id": "1", "word": "gato", "status": "active"}]""")

        val error = assertThrows(IllegalStateException::class.java) { cards.requireById("zzz") }

        assertThat(error).hasMessageThat().isEqualTo("No se encontró la tarjeta.")
    }
}
