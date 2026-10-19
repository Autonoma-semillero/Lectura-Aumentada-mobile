package co.edu.uniautonoma.inclusivereadingar.presentation.webar

import co.edu.uniautonoma.inclusivereadingar.config.WebArConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WebArContainerTest {
    @Test
    fun mountsOnlyWhenCardIsShowingAndCameraHardwareExists() {
        assertThat(
            shouldMountArWebView(showingCard = true, hasCameraHardware = true, arUnavailable = false)
        ).isTrue()
    }

    @Test
    fun doesNotMountWhenCardIsNotShowing() {
        assertThat(
            shouldMountArWebView(showingCard = false, hasCameraHardware = true, arUnavailable = false)
        ).isFalse()
    }

    @Test
    fun fallsBackToPlainTextWhenDeviceHasNoCameraHardware() {
        assertThat(
            shouldMountArWebView(showingCard = true, hasCameraHardware = false, arUnavailable = false)
        ).isFalse()
    }

    @Test
    fun fallsBackToPlainTextOncePermissionOrRuntimeMarkedArUnavailable() {
        assertThat(
            shouldMountArWebView(showingCard = true, hasCameraHardware = true, arUnavailable = true)
        ).isFalse()
    }

    @Test
    fun floatingCardAndMarkerScreenLoadDifferentDocuments() {
        assertThat(WebArConfig.WEB_AR_FLOAT_URL).isNotEqualTo(WebArConfig.WEB_AR_URL)
        assertThat(WebArConfig.WEB_AR_FLOAT_URL).startsWith(WebArConfig.TRUSTED_ORIGIN)
    }
}
