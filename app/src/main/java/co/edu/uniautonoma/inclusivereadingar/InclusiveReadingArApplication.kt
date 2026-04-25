package co.edu.uniautonoma.inclusivereadingar

import android.app.Application
import android.content.Context
import co.edu.uniautonoma.inclusivereadingar.di.AppContainer

class InclusiveReadingArApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}

fun Context.appContainer(): AppContainer {
    return (applicationContext as InclusiveReadingArApplication).appContainer
}
