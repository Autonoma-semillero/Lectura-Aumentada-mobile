package co.edu.uniautonoma.inclusivereadingar.presentation.screens

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WebArResumeCoordinatorTest {
    @Test
    fun readyCameraPause_requestsOneDocumentRestartOnResume() {
        val coordinator = WebArResumeCoordinator()

        coordinator.onPause(wasCameraReady = true)

        assertThat(coordinator.consumeRestartOnResume()).isTrue()
        assertThat(coordinator.consumeRestartOnResume()).isFalse()
    }

    @Test
    fun stopOrReload_cancelsThePendingResumeRestart() {
        val coordinator = WebArResumeCoordinator()
        coordinator.onPause(wasCameraReady = true)

        coordinator.cancelRestart()

        assertThat(coordinator.consumeRestartOnResume()).isFalse()
    }

    @Test
    fun pauseBeforeCameraReady_doesNotRestartUnlessReadinessArrivesWhilePaused() {
        val coordinator = WebArResumeCoordinator()
        coordinator.onPause(wasCameraReady = false)

        assertThat(coordinator.consumeRestartOnResume()).isFalse()

        coordinator.requestRestart()

        assertThat(coordinator.consumeRestartOnResume()).isTrue()
    }

    @Test
    fun bridgeGate_rejectsStaleOrReloadingDocumentEvents() {
        assertThat(
            shouldProcessWebArBridgeEvent(
                type = "marker-found",
                messageSessionId = "old-session",
                activeSessionId = "new-session",
                acceptsSessionEvents = true,
                isDocumentReloading = false,
                isLifecycleResumed = true
            )
        ).isFalse()
        assertThat(
            shouldProcessWebArBridgeEvent(
                type = "camera-ready",
                messageSessionId = null,
                activeSessionId = "new-session",
                acceptsSessionEvents = true,
                isDocumentReloading = false,
                isLifecycleResumed = true
            )
        ).isFalse()
        assertThat(
            shouldProcessWebArBridgeEvent(
                type = "camera-ready",
                messageSessionId = "new-session",
                activeSessionId = "new-session",
                acceptsSessionEvents = true,
                isDocumentReloading = true,
                isLifecycleResumed = true
            )
        ).isFalse()
    }

    @Test
    fun bridgeGate_allowsPausedCameraReadyButNotOtherSessionEvents() {
        assertThat(
            shouldProcessWebArBridgeEvent(
                type = "camera-ready",
                messageSessionId = "session-4",
                activeSessionId = "session-4",
                acceptsSessionEvents = true,
                isDocumentReloading = false,
                isLifecycleResumed = false
            )
        ).isTrue()
        assertThat(
            shouldProcessWebArBridgeEvent(
                type = "model-ready",
                messageSessionId = "session-4",
                activeSessionId = "session-4",
                acceptsSessionEvents = true,
                isDocumentReloading = false,
                isLifecycleResumed = false
            )
        ).isFalse()
    }

    @Test
    fun bridgeGate_keepsExplicitRetryAvailableAfterRuntimeError() {
        assertThat(
            shouldProcessWebArBridgeEvent(
                type = "camera-retry-requested",
                messageSessionId = "session-4",
                activeSessionId = "session-4",
                acceptsSessionEvents = false,
                isDocumentReloading = false,
                isLifecycleResumed = true
            )
        ).isTrue()
    }

    @Test
    fun reloadGate_allowsOnlyTheFirstCallbackForTheCurrentDocument() {
        val gate = WebArReloadGate(generation = 7)

        assertThat(gate.tryAcquire(currentGeneration = 7, isCurrentWebView = true)).isTrue()
        assertThat(gate.tryAcquire(currentGeneration = 7, isCurrentWebView = true)).isFalse()
    }

    @Test
    fun reloadGate_rejectsStaleGenerationAndReplacedWebView() {
        assertThat(
            WebArReloadGate(generation = 7).tryAcquire(
                currentGeneration = 8,
                isCurrentWebView = true
            )
        ).isFalse()
        assertThat(
            WebArReloadGate(generation = 7).tryAcquire(
                currentGeneration = 7,
                isCurrentWebView = false
            )
        ).isFalse()
    }
}
