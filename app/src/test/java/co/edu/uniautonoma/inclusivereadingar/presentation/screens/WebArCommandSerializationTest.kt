package co.edu.uniautonoma.inclusivereadingar.presentation.screens

import co.edu.uniautonoma.inclusivereadingar.domain.model.ArAsset
import co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel.WebArCommand
import co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel.WebArWordTarget
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WebArCommandSerializationTest {
    @Test
    fun fuzzyAssetReady_serializesTheOcrWordSeparatelyFromTheCanonicalAssetWord() {
        val asset = ArAsset(
            id = "asset-gato",
            learningUnitId = "unit-1",
            markerId = "demo-animales-gato",
            word = "gato",
            model3dUrl = "https://cdn.example.test/gato.glb",
            audioUrl = null,
            accessibilityLabel = "Modelo tridimensional de un gato"
        )
        val target = WebArWordTarget(
            word = "pato",
            confidence = 0.91f,
            centerX = 0.42f,
            centerY = 0.55f
        )

        val command = WebArCommand.AssetReady(asset, target)
        val targetFields = checkNotNull(command.wordTarget).toNativeTargetFields()

        assertThat(targetFields["word"]).isEqualTo("pato")
        assertThat(command.asset.word).isEqualTo("gato")
    }
}
