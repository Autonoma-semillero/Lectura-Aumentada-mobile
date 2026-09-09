package co.edu.uniautonoma.inclusivereadingar.domain.ocr

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OcrWordDetectionTest {
    @Test
    fun normalize_ignoresCaseAccentsAndPunctuation() {
        assertThat(OcrWordNormalizer.normalize("  ¡ÁRBOL! ")).isEqualTo("arbol")
    }

    @Test
    fun normalize_preservesEnyeAsADistinctLetter() {
        assertThat(OcrWordNormalizer.normalize("NIÑO")).isEqualTo("niño")
        assertThat(OcrWordNormalizer.normalize("NINO")).isEqualTo("nino")
    }

    @Test
    fun normalize_preservesDecomposedEnyeAndFoldsCompatibilityCharacters() {
        assertThat(OcrWordNormalizer.normalize("NIN\u0303O")).isEqualTo("niño")
        assertThat(OcrWordNormalizer.normalize("ＡＲＢＯＬ")).isEqualTo("arbol")
    }

    @Test
    fun normalize_preservesCanonicalWordSeparatorsAndUnicodeLetters() {
        assertThat(OcrWordNormalizer.normalize("¡CO-OPERAR!")).isEqualTo("co-operar")
        assertThat(OcrWordNormalizer.normalize("L’ARBRE")).isEqualTo("l'arbre")
        assertThat(OcrWordNormalizer.normalize("  CASA   AZUL  ")).isEqualTo("casa azul")
        assertThat(OcrWordNormalizer.normalize("ПРИВЕТ")).isEqualTo("привет")
    }

    @Test
    fun stabilizer_requiresTwoConfidentFramesAndDebouncesTheActiveWord() {
        val stabilizer = OcrWordStabilizer()
        val candidate = candidate("Árbol", confidence = 0.88f)

        assertThat(stabilizer.update(listOf(candidate))).isEmpty()
        assertThat(stabilizer.update(listOf(candidate))).containsExactly(
            OcrWordEvent.Detected(candidate.copy(word = "arbol"))
        )
        assertThat(stabilizer.update(listOf(candidate))).isEmpty()
    }

    @Test
    fun stabilizer_usesLossGraceBeforeClearingTheWord() {
        val stabilizer = OcrWordStabilizer()
        val candidate = candidate("gato", confidence = 0.94f)
        stabilizer.update(listOf(candidate))
        stabilizer.update(listOf(candidate))

        assertThat(stabilizer.update(emptyList())).isEmpty()
        assertThat(stabilizer.update(emptyList())).isEmpty()
        assertThat(stabilizer.update(emptyList())).containsExactly(OcrWordEvent.Lost("gato"))
    }

    @Test
    fun stabilizer_rejectsLowConfidenceNoise() {
        val stabilizer = OcrWordStabilizer()

        repeat(3) {
            assertThat(stabilizer.update(listOf(candidate("ruido", 0.30f)))).isEmpty()
        }
    }

    @Test
    fun stabilizer_switchesDirectlyWhenTheOldWordDisappearsAndANewOneRepeats() {
        val stabilizer = OcrWordStabilizer()
        val gato = candidate("gato", 0.92f)
        val arbol = candidate("árbol", 0.89f, centerX = 0.46f)
        stabilizer.update(listOf(gato))
        stabilizer.update(listOf(gato))

        assertThat(stabilizer.update(listOf(arbol))).isEmpty()
        assertThat(stabilizer.update(listOf(arbol))).containsExactly(
            OcrWordEvent.Lost("gato"),
            OcrWordEvent.Detected(arbol.copy(word = "arbol"))
        ).inOrder()
        assertThat(stabilizer.update(listOf(arbol))).isEmpty()
    }

    @Test
    fun stabilizer_doesNotSwitchToASimilarCompetingWordWhileActiveIsVisible() {
        val stabilizer = OcrWordStabilizer()
        val gato = candidate("gato", 0.91f)
        val ruido = candidate("texto", 0.92f, centerX = 0.52f)
        stabilizer.update(listOf(gato))
        stabilizer.update(listOf(gato))

        repeat(3) {
            assertThat(stabilizer.update(listOf(gato, ruido))).isEmpty()
        }
    }

    @Test
    fun stabilizer_switchesWhenTheAlternativeIsClearlyMoreCentralForTwoFrames() {
        val stabilizer = OcrWordStabilizer()
        val edgeWord = candidate("gato", 0.90f, centerX = 0.10f)
        val centralWord = candidate("árbol", 0.88f, centerX = 0.50f)
        stabilizer.update(listOf(edgeWord))
        stabilizer.update(listOf(edgeWord))

        assertThat(stabilizer.update(listOf(edgeWord, centralWord))).isEmpty()
        assertThat(stabilizer.update(listOf(edgeWord, centralWord))).containsExactly(
            OcrWordEvent.Lost("gato"),
            OcrWordEvent.Detected(centralWord.copy(word = "arbol"))
        ).inOrder()
    }

    @Test
    fun stabilizer_publishesCumulativeMovementFromTheLastReportedPosition() {
        val stabilizer = OcrWordStabilizer(movementThreshold = 0.04f)
        val initial = candidate("gato", 0.91f, centerX = 0.50f)
        stabilizer.update(listOf(initial))
        stabilizer.update(listOf(initial))

        assertThat(stabilizer.update(listOf(initial.copy(centerX = 0.52f)))).isEmpty()
        val moved = initial.copy(centerX = 0.545f)
        assertThat(stabilizer.update(listOf(moved))).containsExactly(
            OcrWordEvent.Updated(moved)
        )
        assertThat(stabilizer.update(listOf(moved.copy(centerX = 0.56f)))).isEmpty()
    }

    private fun candidate(
        word: String,
        confidence: Float,
        centerX: Float = 0.5f,
        centerY: Float = 0.5f
    ) = OcrWordCandidate(
        word = word,
        confidence = confidence,
        centerX = centerX,
        centerY = centerY,
        relativeArea = 0.03f
    )
}
