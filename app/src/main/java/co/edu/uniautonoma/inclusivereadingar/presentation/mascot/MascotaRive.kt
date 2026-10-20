package co.edu.uniautonoma.inclusivereadingar.presentation.mascot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.rive.Fit
import app.rive.Result
import app.rive.Rive
import app.rive.RiveFileSource
import app.rive.ViewModelInstance
import app.rive.ViewModelSource
import app.rive.rememberRiveFile
import app.rive.rememberRiveWorker
import app.rive.rememberViewModelInstanceResult
import co.edu.uniautonoma.inclusivereadingar.R

/** Adapter from the Rive view model instance to the framework-free [MascotInputs]. */
private class RiveMascotInputs(private val instance: ViewModelInstance) : MascotInputs {
    override fun fireTrigger(name: String) = instance.fireTrigger(name)
    override fun setNumber(name: String, value: Float) = instance.setNumber(name, value)
}

private const val MASCOT_VIEW_MODEL = "MascotVM"

/**
 * The animated mascot. It renders `res/raw/mascota.riv` and connects the file's `MascotVM`
 * view model to [controller]; drive it only through the controller / [MascotViewModel].
 *
 * Rendering pauses automatically with the lifecycle; [paused] lets a screen freeze it too.
 * Each instance owns one Rive worker, so avoid composing several at once.
 */
@Composable
fun MascotaRive(
    controller: MascotController,
    modifier: Modifier = Modifier,
    paused: Boolean = false,
    description: String = "Mascota",
) {
    val worker = rememberRiveWorker()
    val fileResult = rememberRiveFile(RiveFileSource.RawRes.from(R.raw.mascota), worker)

    Box(
        modifier = modifier.semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        // Loading and error states stay empty on purpose: the mascot is decoration and
        // must never block or crash the learning screen.
        if (fileResult is Result.Success) {
            val riveFile = fileResult.value
            val instanceResult = rememberViewModelInstanceResult(
                riveFile,
                ViewModelSource.Named(MASCOT_VIEW_MODEL).defaultInstance(),
            )
            if (instanceResult is Result.Success) {
                val instance = instanceResult.value
                DisposableEffect(instance, controller) {
                    controller.attach(RiveMascotInputs(instance))
                    onDispose { controller.detach() }
                }
                Rive(
                    riveFile,
                    modifier = Modifier.fillMaxSize(),
                    playing = !paused,
                    viewModelInstance = instance,
                    fit = Fit.Contain(),
                )
            }
        }
    }
}
