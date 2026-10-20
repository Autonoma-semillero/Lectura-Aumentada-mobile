package co.edu.uniautonoma.inclusivereadingar

import android.app.Application
import android.content.Context
import app.rive.runtime.kotlin.core.Rive
import co.edu.uniautonoma.inclusivereadingar.di.AppContainer

class InclusiveReadingArApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Loads the Rive native runtime once so the mascot can render on first use.
        Rive.init(this)
    }
}

fun Context.appContainer(): AppContainer {
    return (applicationContext as InclusiveReadingArApplication).appContainer
}
