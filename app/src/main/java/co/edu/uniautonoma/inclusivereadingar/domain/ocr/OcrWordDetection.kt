package co.edu.uniautonoma.inclusivereadingar.domain.ocr

import java.text.Normalizer
import java.util.Locale

data class OcrWordCandidate(
    val word: String,
    val confidence: Float,
    val centerX: Float,
    val centerY: Float,
    val relativeArea: Float = 0f
)

sealed interface OcrWordEvent {
    data class Detected(val candidate: OcrWordCandidate) : OcrWordEvent
    data class Updated(val candidate: OcrWordCandidate) : OcrWordEvent
    data class Lost(val word: String) : OcrWordEvent
}

/**
 * Converts OCR output and API input to the same stable key. Images are never involved here:
 * only the normalized word is allowed to leave the on-device recognition pipeline.
 */
object OcrWordNormalizer {
    private val combiningMarks = Regex("\\p{M}+")
    private val whitespace = Regex("[\\s\\p{Z}]+")
    private val unsupportedCharacters = Regex("[^\\p{L}\\p{N} '\\-]+")
    private val validCanonicalWord = Regex(
        "^[\\p{L}\\p{N}]+(?:(?: +|['-])[\\p{L}\\p{N}]+)*$"
    )

    fun normalize(rawValue: String): String? {
        val lowercase = Normalizer.normalize(rawValue, Normalizer.Form.NFKC)
            .trim()
            .replace(whitespace, " ")
            .lowercase(SPANISH_COLOMBIA)
            .normalizeOcrSeparators()
            .replace('ñ', PRESERVED_ENYE)
        val normalized = Normalizer.normalize(
            lowercase,
            Normalizer.Form.NFKD
        )
            .replace(combiningMarks, "")
            .replace(PRESERVED_ENYE, 'ñ')
            .replace(unsupportedCharacters, "")
            .replace(whitespace, " ")
            .trim(' ', '\'', '-')
            .let { Normalizer.normalize(it, Normalizer.Form.NFC) }

        return normalized.takeIf {
            it.length in MIN_WORD_LENGTH..MAX_WORD_LENGTH && validCanonicalWord.matches(it)
        }
    }

    private fun String.normalizeOcrSeparators(): String = this
        .replace(Regex("[\u2018\u2019\u02BC\uFF07]"), "'")
        .replace(Regex("[\u2010\u2011\u2012\u2013\u2014\u2212\uFF0D]"), "-")

    const val MIN_WORD_LENGTH = 1
    const val MAX_WORD_LENGTH = 64
    private const val PRESERVED_ENYE = '\uE000'
    private val SPANISH_COLOMBIA = Locale.forLanguageTag("es-CO")
}

/** Mirrors the backend's conservative OCR-to-asset response contract. */
object OcrWordMatchPolicy {
    const val MIN_FUZZY_WORD_LENGTH = 3

    fun acceptsAssetResponse(requestedWord: String, assetWord: String): Boolean {
        val requested = OcrWordNormalizer.normalize(requestedWord) ?: return false
        val candidate = OcrWordNormalizer.normalize(assetWord) ?: return false
        if (requested == candidate) return true

        val requestedCharacters = requested.codePoints().toArray()
        val candidateCharacters = candidate.codePoints().toArray()
        if (requestedCharacters.size < MIN_FUZZY_WORD_LENGTH ||
            candidateCharacters.size < MIN_FUZZY_WORD_LENGTH
        ) {
            return false
        }
        return levenshteinDistance(requestedCharacters, candidateCharacters) == 1
    }

    private fun levenshteinDistance(left: IntArray, right: IntArray): Int {
        var previousRow = IntArray(right.size + 1) { it }
        for (leftIndex in left.indices) {
            val currentRow = IntArray(right.size + 1)
            currentRow[0] = leftIndex + 1
            for (rightIndex in right.indices) {
                val insertion = currentRow[rightIndex] + 1
                val deletion = previousRow[rightIndex + 1] + 1
                val substitution = previousRow[rightIndex] +
                    if (left[leftIndex] == right[rightIndex]) 0 else 1
                currentRow[rightIndex + 1] = minOf(insertion, deletion, substitution)
            }
            previousRow = currentRow
        }
        return previousRow[right.size]
    }
}

/**
 * Requires the same sufficiently-confident word in consecutive frames and applies loss grace.
 * This prevents OCR noise from causing requests or model flicker on every camera frame.
 */
class OcrWordStabilizer(
    private val minimumConfidence: Float = DEFAULT_MINIMUM_CONFIDENCE,
    private val confirmationFrames: Int = DEFAULT_CONFIRMATION_FRAMES,
    private val lossFrames: Int = DEFAULT_LOSS_FRAMES,
    private val movementThreshold: Float = DEFAULT_MOVEMENT_THRESHOLD
) {
    private var pending: OcrWordCandidate? = null
    private var pendingHits = 0
    private var active: OcrWordCandidate? = null
    private var lastPublishedPosition: OcrWordCandidate? = null
    private var misses = 0

    init {
        require(minimumConfidence in 0f..1f)
        require(confirmationFrames > 0)
        require(lossFrames > 0)
        require(movementThreshold in 0f..1f)
    }

    fun update(rawCandidates: List<OcrWordCandidate>): List<OcrWordEvent> {
        val candidates = rawCandidates
            .asSequence()
            .mapNotNull(::validated)
            .groupBy(OcrWordCandidate::word)
            .mapNotNull { (_, matches) -> matches.maxByOrNull(::score) }

        val current = active
        if (current != null) {
            val match = candidates.firstOrNull { it.word == current.word }
            val alternative = candidates
                .asSequence()
                .filter { it.word != current.word }
                .maxByOrNull(::score)

            if (match != null) {
                active = match
                misses = 0
                val clearReplacement = alternative?.takeIf {
                    isClearReplacement(current = match, alternative = it)
                }
                val replacementEvents = confirmReplacement(current.word, clearReplacement)
                if (replacementEvents.isNotEmpty()) return replacementEvents

                val published = lastPublishedPosition
                if (published == null || positionDistance(published, match) >= movementThreshold) {
                    lastPublishedPosition = match
                    return listOf(OcrWordEvent.Updated(match))
                }
                return emptyList()
            }

            misses += 1
            val replacementEvents = confirmReplacement(current.word, alternative)
            if (replacementEvents.isNotEmpty()) return replacementEvents
            if (misses < lossFrames) return emptyList()

            active = null
            lastPublishedPosition = null
            clearPending()
            misses = 0
            return listOf(OcrWordEvent.Lost(current.word))
        }

        val best = candidates.maxByOrNull(::score)
        if (best == null) {
            clearPending()
            return emptyList()
        }

        if (!confirm(best)) return emptyList()

        active = best
        lastPublishedPosition = best
        clearPending()
        misses = 0
        return listOf(OcrWordEvent.Detected(best))
    }

    fun reset(): List<OcrWordEvent> {
        val lostWord = active?.word
        active = null
        lastPublishedPosition = null
        clearPending()
        misses = 0
        return lostWord?.let { listOf(OcrWordEvent.Lost(it)) }.orEmpty()
    }

    private fun confirmReplacement(
        currentWord: String,
        replacement: OcrWordCandidate?
    ): List<OcrWordEvent> {
        if (replacement == null) {
            clearPending()
            return emptyList()
        }
        if (!confirm(replacement)) return emptyList()

        active = replacement
        lastPublishedPosition = replacement
        clearPending()
        misses = 0
        return listOf(
            OcrWordEvent.Lost(currentWord),
            OcrWordEvent.Detected(replacement)
        )
    }

    private fun confirm(candidate: OcrWordCandidate): Boolean {
        if (pending?.word == candidate.word) {
            pending = candidate
            pendingHits += 1
        } else {
            pending = candidate
            pendingHits = 1
        }
        return pendingHits >= confirmationFrames
    }

    private fun clearPending() {
        pending = null
        pendingHits = 0
    }

    private fun isClearReplacement(
        current: OcrWordCandidate,
        alternative: OcrWordCandidate
    ): Boolean {
        val currentScore = score(current)
        val alternativeScore = score(alternative)
        if (alternativeScore >= currentScore + SWITCH_SCORE_MARGIN) return true

        val isClearlyMoreCentral =
            centerDistance(current) - centerDistance(alternative) >= SWITCH_CENTER_MARGIN
        return isClearlyMoreCentral &&
            alternativeScore >= currentScore - SWITCH_CENTER_SCORE_TOLERANCE
    }

    private fun validated(candidate: OcrWordCandidate): OcrWordCandidate? {
        val word = OcrWordNormalizer.normalize(candidate.word) ?: return null
        if (!candidate.confidence.isFinite() || candidate.confidence < minimumConfidence) return null
        if (!candidate.centerX.isFinite() || !candidate.centerY.isFinite()) return null
        val validated = candidate.copy(
            word = word,
            confidence = candidate.confidence.coerceIn(0f, 1f),
            centerX = candidate.centerX.coerceIn(0f, 1f),
            centerY = candidate.centerY.coerceIn(0f, 1f),
            relativeArea = candidate.relativeArea.coerceIn(0f, 1f)
        )
        if (validated.confidence < STANDARD_CONFIDENCE &&
            (validated.word.length < LOW_CONFIDENCE_MIN_WORD_LENGTH ||
                validated.relativeArea < LOW_CONFIDENCE_MIN_RELATIVE_AREA ||
                centerDistance(validated) > LOW_CONFIDENCE_MAX_CENTER_DISTANCE)
        ) {
            return null
        }
        return validated
    }

    private fun score(candidate: OcrWordCandidate): Float {
        val centerDistance = centerDistance(candidate)
        val centerBonus = (1f - (centerDistance / 0.71f)).coerceIn(0f, 1f) * CENTER_WEIGHT
        val areaBonus = (candidate.relativeArea * AREA_SCALE).coerceAtMost(AREA_WEIGHT)
        return candidate.confidence + centerBonus + areaBonus
    }

    private fun centerDistance(candidate: OcrWordCandidate): Float =
        kotlin.math.hypot(
            (candidate.centerX - 0.5f).toDouble(),
            (candidate.centerY - 0.5f).toDouble()
        ).toFloat()

    private fun positionDistance(
        first: OcrWordCandidate,
        second: OcrWordCandidate
    ): Float = kotlin.math.hypot(
        (first.centerX - second.centerX).toDouble(),
        (first.centerY - second.centerY).toDouble()
    ).toFloat()

    companion object {
        // Pencil handwriting commonly scores below ML Kit's printed-text confidence.
        // Low-confidence candidates are constrained by length, size and central position.
        const val DEFAULT_MINIMUM_CONFIDENCE = 0.32f
        const val DEFAULT_CONFIRMATION_FRAMES = 2
        const val DEFAULT_LOSS_FRAMES = 3
        const val DEFAULT_MOVEMENT_THRESHOLD = 0.04f
        private const val STANDARD_CONFIDENCE = 0.60f
        private const val LOW_CONFIDENCE_MIN_WORD_LENGTH = 3
        private const val LOW_CONFIDENCE_MIN_RELATIVE_AREA = 0.005f
        private const val LOW_CONFIDENCE_MAX_CENTER_DISTANCE = 0.35f
        private const val CENTER_WEIGHT = 0.08f
        private const val AREA_SCALE = 4f
        private const val AREA_WEIGHT = 0.12f
        private const val SWITCH_SCORE_MARGIN = 0.10f
        private const val SWITCH_CENTER_MARGIN = 0.18f
        private const val SWITCH_CENTER_SCORE_TOLERANCE = 0.03f
    }
}
