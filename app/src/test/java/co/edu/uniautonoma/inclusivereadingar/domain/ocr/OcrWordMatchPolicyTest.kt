package co.edu.uniautonoma.inclusivereadingar.domain.ocr

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OcrWordMatchPolicyTest {
    @Test
    fun acceptsExactAndSingleEditAssetResponses() {
        assertThat(OcrWordMatchPolicy.acceptsAssetResponse("GATO", "gáto")).isTrue()
        assertThat(OcrWordMatchPolicy.acceptsAssetResponse("pato", "gato")).isTrue()
        assertThat(OcrWordMatchPolicy.acceptsAssetResponse("gatos", "gato")).isTrue()
    }

    @Test
    fun rejectsUnrelatedOrShortFuzzyAssetResponses() {
        assertThat(OcrWordMatchPolicy.acceptsAssetResponse("pato", "perro")).isFalse()
        assertThat(OcrWordMatchPolicy.acceptsAssetResponse("no", "yo")).isFalse()
        assertThat(OcrWordMatchPolicy.acceptsAssetResponse("sol", "so")).isFalse()
    }

    @Test
    fun keepsShortExactAssetResponsesValid() {
        assertThat(OcrWordMatchPolicy.acceptsAssetResponse("YO", "yo")).isTrue()
    }
}
