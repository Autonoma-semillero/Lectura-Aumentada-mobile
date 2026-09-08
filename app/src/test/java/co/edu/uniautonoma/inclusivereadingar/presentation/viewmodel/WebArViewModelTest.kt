package co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel

import co.edu.uniautonoma.inclusivereadingar.MainDispatcherRule
import co.edu.uniautonoma.inclusivereadingar.domain.model.ArAsset
import co.edu.uniautonoma.inclusivereadingar.domain.repository.ArAssetRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
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
        assertThat(viewModel.uiState.value.outboundCommand?.command)
            .isEqualTo(WebArCommand.AssetReady(gatoAsset))
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

    private class FakeArAssetRepository(
        private val results: Map<String, ArAsset?>
    ) : ArAssetRepository {
        val requests = mutableListOf<String>()

        override suspend fun findByMarker(markerId: String): ArAsset? {
            requests += markerId
            return results[markerId]
        }
    }
}
