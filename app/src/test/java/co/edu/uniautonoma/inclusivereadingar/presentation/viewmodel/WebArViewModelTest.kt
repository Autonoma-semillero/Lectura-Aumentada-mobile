package co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel

import co.edu.uniautonoma.inclusivereadingar.MainDispatcherRule
import co.edu.uniautonoma.inclusivereadingar.domain.model.ArAsset
import co.edu.uniautonoma.inclusivereadingar.domain.repository.ArAssetRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebArViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val gatoAsset = ArAsset(
        id = "asset-1",
        learningUnitId = "unit-1",
        markerId = "demo-animales-gato",
        word = "gato",
        model3dUrl = "https://cdn.example.test/gato.glb",
        audioUrl = "https://cdn.example.test/gato.mp3",
        accessibilityLabel = "Modelo tridimensional de un gato"
    )

    @Test
    fun markerFound_fetchesAndPublishesTheAssociatedAsset() = runTest {
        val repository = FakeArAssetRepository(results = mapOf(gatoAsset.markerId to gatoAsset))
        val viewModel = WebArViewModel(repository)

        viewModel.onMarkerFound(gatoAsset.markerId)
        advanceUntilIdle()

        assertThat(repository.requests).containsExactly(gatoAsset.markerId)
        assertThat(viewModel.uiState.value.asset).isEqualTo(gatoAsset)
        assertThat(viewModel.uiState.value.isAssetLoading).isTrue()
        assertThat(viewModel.uiState.value.outboundCommand?.command)
            .isEqualTo(WebArCommand.AssetReady(gatoAsset))

        viewModel.onModelReady(gatoAsset.id)

        assertThat(viewModel.uiState.value.isAssetLoading).isFalse()
    }

    @Test
    fun repeatedMarkerEvents_makeOnlyOneRequestWhileActive() = runTest {
        val repository = FakeArAssetRepository(results = mapOf(gatoAsset.markerId to gatoAsset))
        val viewModel = WebArViewModel(repository)

        repeat(5) { viewModel.onMarkerFound(gatoAsset.markerId) }
        advanceUntilIdle()
        repeat(5) { viewModel.onMarkerFound(gatoAsset.markerId) }

        assertThat(repository.requests).containsExactly(gatoAsset.markerId)
    }

    @Test
    fun refoundMarker_usesTheSessionCacheAfterItWasLost() = runTest {
        val repository = FakeArAssetRepository(results = mapOf(gatoAsset.markerId to gatoAsset))
        val viewModel = WebArViewModel(repository)

        viewModel.onMarkerFound(gatoAsset.markerId)
        advanceUntilIdle()
        viewModel.onMarkerLost(gatoAsset.markerId)
        viewModel.onMarkerFound(gatoAsset.markerId)
        advanceUntilIdle()

        assertThat(repository.requests).containsExactly(gatoAsset.markerId)
        assertThat(viewModel.uiState.value.asset).isEqualTo(gatoAsset)
    }

    @Test
    fun markerWithoutAssociation_isAControlledState() = runTest {
        val repository = FakeArAssetRepository(results = emptyMap())
        val viewModel = WebArViewModel(repository)

        viewModel.onMarkerFound("unknown-marker")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.markerWithoutAssociation).isTrue()
        assertThat(viewModel.uiState.value.errorMessage).isNull()
        assertThat(viewModel.uiState.value.outboundCommand?.command)
            .isEqualTo(WebArCommand.MarkerNotFound("unknown-marker"))
    }

    @Test
    fun markerLost_clearsTheCurrentAsset() = runTest {
        val repository = FakeArAssetRepository(results = mapOf(gatoAsset.markerId to gatoAsset))
        val viewModel = WebArViewModel(repository)
        viewModel.onMarkerFound(gatoAsset.markerId)
        advanceUntilIdle()

        viewModel.onMarkerLost(gatoAsset.markerId)

        assertThat(viewModel.uiState.value.activeMarkerId).isNull()
        assertThat(viewModel.uiState.value.asset).isNull()
        assertThat(viewModel.uiState.value.outboundCommand?.command)
            .isEqualTo(WebArCommand.ClearMarker(gatoAsset.markerId))
    }

    @Test
    fun invalidMarker_isRejectedBeforeCallingTheRepository() = runTest {
        val repository = FakeArAssetRepository(results = emptyMap())
        val viewModel = WebArViewModel(repository)

        viewModel.onMarkerFound("<script>")
        advanceUntilIdle()

        assertThat(repository.requests).isEmpty()
        assertThat(viewModel.uiState.value.errorMessage).isNotNull()
    }

    @Test
    fun ocrWord_isNormalizedAndPublishesAnAtomicWordTarget() = runTest {
        val repository = FakeArAssetRepository(
            results = emptyMap(),
            wordResults = mapOf("arbol" to gatoAsset.copy(word = "Árbol"))
        )
        val viewModel = WebArViewModel(repository)

        viewModel.onOcrWordDetected("  ÁRBOL. ", 0.91f, 0.4f, 0.6f)
        advanceUntilIdle()

        assertThat(repository.wordRequests).containsExactly("arbol")
        assertThat(viewModel.uiState.value.activeWordTarget?.word).isEqualTo("arbol")
        assertThat(viewModel.uiState.value.outboundCommand?.command).isEqualTo(
            WebArCommand.AssetReady(
                gatoAsset.copy(word = "Árbol"),
                WebArWordTarget("arbol", 0.91f, 0.4f, 0.6f)
            )
        )
    }

    @Test
    fun fuzzyWordAsset_keepsTheOcrTargetAcrossPositionUpdates() = runTest {
        val repository = FakeArAssetRepository(
            results = emptyMap(),
            wordResults = mapOf("pato" to gatoAsset)
        )
        val viewModel = WebArViewModel(repository)
        val detectedTarget = WebArWordTarget("pato", 0.91f, 0.42f, 0.55f)

        viewModel.onOcrWordDetected(
            detectedTarget.word,
            detectedTarget.confidence,
            detectedTarget.centerX,
            detectedTarget.centerY
        )
        advanceUntilIdle()

        assertThat(repository.wordRequests).containsExactly("pato")
        assertThat(viewModel.uiState.value.asset?.word).isEqualTo("gato")
        assertThat(viewModel.uiState.value.outboundCommand?.command)
            .isEqualTo(WebArCommand.AssetReady(gatoAsset, detectedTarget))

        viewModel.onModelReady(gatoAsset.id)
        val movedTarget = WebArWordTarget("pato", 0.93f, 0.57f, 0.61f)
        viewModel.onOcrWordPositionUpdated(
            movedTarget.word,
            movedTarget.confidence,
            movedTarget.centerX,
            movedTarget.centerY
        )

        assertThat(repository.wordRequests).containsExactly("pato")
        assertThat(viewModel.uiState.value.asset).isEqualTo(gatoAsset)
        assertThat(viewModel.uiState.value.activeWordTarget).isEqualTo(movedTarget)
        assertThat(viewModel.uiState.value.outboundCommand?.command)
            .isEqualTo(WebArCommand.ActivateWordTarget(movedTarget))
    }

    @Test
    fun lowConfidenceOcrWord_isIgnored() = runTest {
        val repository = FakeArAssetRepository(results = emptyMap())
        val viewModel = WebArViewModel(repository)

        viewModel.onOcrWordDetected("árbol", 0.30f, 0.5f, 0.5f)
        advanceUntilIdle()

        assertThat(repository.wordRequests).isEmpty()
        assertThat(viewModel.uiState.value.activeWordTarget).isNull()
    }

    @Test
    fun stabilizedHandwrittenWord_isAcceptedByTheViewModel() = runTest {
        val repository = FakeArAssetRepository(
            results = emptyMap(),
            wordResults = mapOf("gato" to gatoAsset)
        )
        val viewModel = WebArViewModel(repository)

        viewModel.onOcrWordDetected("GATO", 0.40f, 0.45f, 0.57f)
        advanceUntilIdle()

        assertThat(repository.wordRequests).containsExactly("gato")
        assertThat(viewModel.uiState.value.activeWordTarget?.word).isEqualTo("gato")
    }

    @Test
    fun lostOcrWord_clearsItsTargetAndAsset() = runTest {
        val repository = FakeArAssetRepository(
            results = emptyMap(),
            wordResults = mapOf("gato" to gatoAsset)
        )
        val viewModel = WebArViewModel(repository)
        viewModel.onOcrWordDetected("gato", 0.90f, 0.5f, 0.5f)
        advanceUntilIdle()

        viewModel.onOcrWordLost("GATO")

        assertThat(viewModel.uiState.value.activeWordTarget).isNull()
        assertThat(viewModel.uiState.value.asset).isNull()
        assertThat(viewModel.uiState.value.outboundCommand?.command)
            .isEqualTo(WebArCommand.ClearWordTarget("gato"))
    }

    @Test
    fun movedOcrWord_repositionsWithoutRequestingTheAssetAgain() = runTest {
        val repository = FakeArAssetRepository(
            results = emptyMap(),
            wordResults = mapOf("gato" to gatoAsset)
        )
        val viewModel = WebArViewModel(repository)
        viewModel.onOcrWordDetected("gato", 0.90f, 0.40f, 0.50f)
        advanceUntilIdle()

        viewModel.onOcrWordPositionUpdated("GATO", 0.93f, 0.62f, 0.58f)

        val expectedTarget = WebArWordTarget("gato", 0.93f, 0.62f, 0.58f)
        assertThat(repository.wordRequests).containsExactly("gato")
        assertThat(viewModel.uiState.value.asset).isEqualTo(gatoAsset)
        assertThat(viewModel.uiState.value.activeWordTarget).isEqualTo(expectedTarget)
        assertThat(viewModel.uiState.value.isAssetLoading).isTrue()
        assertThat(viewModel.uiState.value.outboundCommand?.command)
            .isInstanceOf(WebArCommand.AssetReady::class.java)

        viewModel.onModelReady(gatoAsset.id)

        assertThat(viewModel.uiState.value.isAssetLoading).isFalse()
        assertThat(viewModel.uiState.value.outboundCommand?.command)
            .isEqualTo(WebArCommand.ActivateWordTarget(expectedTarget))
    }

    @Test
    fun movedOcrWordDuringLookup_isIncludedInTheAtomicAssetCommand() = runTest {
        val deferredAsset = CompletableDeferred<ArAsset?>()
        val repository = DeferredWordArAssetRepository(deferredAsset)
        val viewModel = WebArViewModel(repository)
        viewModel.onOcrWordDetected("gato", 0.90f, 0.40f, 0.50f)
        runCurrent()

        viewModel.onOcrWordPositionUpdated("GATO", 0.94f, 0.66f, 0.61f)
        deferredAsset.complete(gatoAsset)
        advanceUntilIdle()

        val latestTarget = WebArWordTarget("gato", 0.94f, 0.66f, 0.61f)
        assertThat(repository.wordRequests).containsExactly("gato")
        assertThat(viewModel.uiState.value.activeWordTarget).isEqualTo(latestTarget)
        assertThat(viewModel.uiState.value.outboundCommand?.command)
            .isEqualTo(WebArCommand.AssetReady(gatoAsset, latestTarget))
    }

    @Test
    fun missingOcrWord_keepsItsPositionInTheAtomicNotFoundCommand() = runTest {
        val deferredAsset = CompletableDeferred<ArAsset?>()
        val repository = DeferredWordArAssetRepository(deferredAsset)
        val viewModel = WebArViewModel(repository)
        val target = WebArWordTarget("arbol", 0.88f, 0.42f, 0.57f)

        viewModel.onOcrWordDetected(
            target.word,
            target.confidence,
            target.centerX,
            target.centerY
        )
        runCurrent()
        val latestTarget = WebArWordTarget("arbol", 0.91f, 0.55f, 0.63f)
        viewModel.onOcrWordPositionUpdated(
            latestTarget.word,
            latestTarget.confidence,
            latestTarget.centerX,
            latestTarget.centerY
        )
        deferredAsset.complete(null)
        advanceUntilIdle()

        assertThat(repository.wordRequests).containsExactly("arbol")
        assertThat(viewModel.uiState.value.outboundCommand?.command)
            .isEqualTo(WebArCommand.WordNotFound(latestTarget))
    }

    @Test
    fun modelError_updatesNativeFeedbackWithoutClearingOcrOrRefetching() = runTest {
        val repository = FakeArAssetRepository(
            results = emptyMap(),
            wordResults = mapOf("gato" to gatoAsset)
        )
        val viewModel = WebArViewModel(repository)
        viewModel.onOcrWordDetected("gato", 0.90f, 0.50f, 0.50f)
        advanceUntilIdle()
        val target = viewModel.uiState.value.activeWordTarget
        val commandId = viewModel.uiState.value.outboundCommand?.id
        if (commandId != null) viewModel.onCommandDelivered(commandId)

        viewModel.onModelError(gatoAsset.id, "El archivo GLB no pudo cargarse")

        assertThat(viewModel.uiState.value.activeWordTarget).isEqualTo(target)
        assertThat(viewModel.uiState.value.asset).isEqualTo(gatoAsset)
        assertThat(viewModel.uiState.value.errorMessage)
            .isEqualTo("El archivo GLB no pudo cargarse")
        assertThat(viewModel.uiState.value.isAssetLoading).isFalse()
        assertThat(viewModel.uiState.value.outboundCommand).isNull()
        assertThat(repository.wordRequests).containsExactly("gato")

        // A duplicate/late completion from the same load cannot clear the reported error.
        viewModel.onModelReady(gatoAsset.id)

        assertThat(viewModel.uiState.value.isAssetLoading).isFalse()
        assertThat(viewModel.uiState.value.errorMessage)
            .isEqualTo("El archivo GLB no pudo cargarse")
        assertThat(viewModel.uiState.value.activeWordTarget).isEqualTo(target)
        assertThat(repository.wordRequests).containsExactly("gato")
    }

    @Test
    fun modelReady_finishesLoadingWithoutClearingOcrOrRefetching() = runTest {
        val repository = FakeArAssetRepository(
            results = emptyMap(),
            wordResults = mapOf("gato" to gatoAsset)
        )
        val viewModel = WebArViewModel(repository)
        viewModel.onOcrWordDetected("gato", 0.90f, 0.50f, 0.50f)
        advanceUntilIdle()
        val target = viewModel.uiState.value.activeWordTarget

        viewModel.onModelReady(gatoAsset.id)

        assertThat(viewModel.uiState.value.isAssetLoading).isFalse()
        assertThat(viewModel.uiState.value.statusMessage).isEqualTo("Mostrando gato")
        assertThat(viewModel.uiState.value.errorMessage).isNull()
        assertThat(viewModel.uiState.value.activeWordTarget).isEqualTo(target)
        assertThat(repository.wordRequests).containsExactly("gato")
    }

    @Test
    fun staleModelEvent_cannotCompleteTheNewActiveAsset() = runTest {
        val treeAsset = gatoAsset.copy(
            id = "asset-2",
            markerId = "demo-naturaleza-arbol",
            word = "arbol",
            model3dUrl = "https://cdn.example.test/arbol.glb"
        )
        val repository = FakeArAssetRepository(
            results = emptyMap(),
            wordResults = mapOf("gato" to gatoAsset, "arbol" to treeAsset)
        )
        val viewModel = WebArViewModel(repository)
        viewModel.onOcrWordDetected("gato", 0.90f, 0.40f, 0.50f)
        advanceUntilIdle()
        viewModel.onOcrWordDetected("árbol", 0.94f, 0.60f, 0.55f)
        advanceUntilIdle()

        viewModel.onModelError(gatoAsset.id, "Finalización anterior")
        viewModel.onModelReady(gatoAsset.id)

        assertThat(viewModel.uiState.value.asset).isEqualTo(treeAsset)
        assertThat(viewModel.uiState.value.isAssetLoading).isTrue()
        assertThat(viewModel.uiState.value.errorMessage).isNull()

        viewModel.onModelReady(treeAsset.id)

        assertThat(viewModel.uiState.value.isAssetLoading).isFalse()
        assertThat(viewModel.uiState.value.statusMessage).isEqualTo("Mostrando arbol")
    }

    @Test
    fun webRuntimeError_clearsTheSessionAndRequiresACameraRestart() = runTest {
        val repository = FakeArAssetRepository(
            results = emptyMap(),
            wordResults = mapOf("gato" to gatoAsset)
        )
        val viewModel = WebArViewModel(repository)
        viewModel.onOcrWordDetected("gato", 0.92f, 0.45f, 0.55f)
        advanceUntilIdle()

        viewModel.onWebError("Fallo del motor AR")

        assertThat(viewModel.uiState.value.activeMarkerId).isNull()
        assertThat(viewModel.uiState.value.activeWordTarget).isNull()
        assertThat(viewModel.uiState.value.asset).isNull()
        assertThat(viewModel.uiState.value.isAssetLoading).isFalse()
        assertThat(viewModel.uiState.value.outboundCommand).isNull()
        assertThat(viewModel.uiState.value.requiresCameraRestart).isTrue()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("Fallo del motor AR")
    }

    @Test
    fun stoppedExperience_clearsNativeTargetAndPendingCommandSynchronously() = runTest {
        val repository = FakeArAssetRepository(
            results = emptyMap(),
            wordResults = mapOf("gato" to gatoAsset)
        )
        val viewModel = WebArViewModel(repository)
        viewModel.onOcrWordDetected("gato", 0.92f, 0.45f, 0.55f)
        advanceUntilIdle()
        viewModel.onModelError(gatoAsset.id, "Error previo")

        viewModel.onExperienceStopped()

        assertThat(viewModel.uiState.value).isEqualTo(WebArUiState())
    }

    private class FakeArAssetRepository(
        private val results: Map<String, ArAsset?>,
        private val wordResults: Map<String, ArAsset?> = emptyMap()
    ) : ArAssetRepository {
        val requests = mutableListOf<String>()
        val wordRequests = mutableListOf<String>()

        override suspend fun findByMarker(markerId: String): ArAsset? {
            requests += markerId
            return results[markerId]
        }

        override suspend fun findByWord(word: String): ArAsset? {
            wordRequests += word
            return wordResults[word]
        }
    }

    private class DeferredWordArAssetRepository(
        private val result: CompletableDeferred<ArAsset?>
    ) : ArAssetRepository {
        val wordRequests = mutableListOf<String>()

        override suspend fun findByMarker(markerId: String): ArAsset? = null

        override suspend fun findByWord(word: String): ArAsset? {
            wordRequests += word
            return result.await()
        }
    }
}
