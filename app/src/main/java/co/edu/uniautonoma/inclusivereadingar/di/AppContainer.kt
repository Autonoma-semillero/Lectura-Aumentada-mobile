package co.edu.uniautonoma.inclusivereadingar.di

import android.content.Context
import co.edu.uniautonoma.inclusivereadingar.data.local.SessionStore
import co.edu.uniautonoma.inclusivereadingar.data.remote.BackendHttpClient
import co.edu.uniautonoma.inclusivereadingar.data.remote.HttpAuthApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.HttpCategoriesApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.HttpProgressApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.HttpWordCardsApi
import co.edu.uniautonoma.inclusivereadingar.data.repository.AuthRepository
import co.edu.uniautonoma.inclusivereadingar.data.repository.StudentContentRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val sessionStore: SessionStore by lazy { SessionStore(appContext) }
    private val httpClient: BackendHttpClient by lazy { BackendHttpClient(sessionStore) }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            authApi = HttpAuthApi(httpClient),
            sessionStore = sessionStore
        )
    }

    val studentContentRepository: StudentContentRepository by lazy {
        StudentContentRepository(
            sessionStore = sessionStore,
            categoriesApi = HttpCategoriesApi(httpClient),
            wordCardsApi = HttpWordCardsApi(httpClient),
            progressApi = HttpProgressApi(httpClient)
        )
    }
}
