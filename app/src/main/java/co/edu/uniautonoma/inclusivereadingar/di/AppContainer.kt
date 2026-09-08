package co.edu.uniautonoma.inclusivereadingar.di

import android.content.Context
import co.edu.uniautonoma.inclusivereadingar.data.local.SessionStore
import co.edu.uniautonoma.inclusivereadingar.data.remote.BackendHttpClient
import co.edu.uniautonoma.inclusivereadingar.data.remote.HttpAuthApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.HttpArAssetsApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.HttpCategoriesApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.HttpDocenteApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.HttpDomanPlansApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.HttpDomanSessionsApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.HttpProgressApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.HttpTeacherApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.HttpWordCardsApi
import co.edu.uniautonoma.inclusivereadingar.data.repository.AuthRepository
import co.edu.uniautonoma.inclusivereadingar.data.repository.BackendArAssetRepository
import co.edu.uniautonoma.inclusivereadingar.data.repository.DocenteProgressRepository
import co.edu.uniautonoma.inclusivereadingar.data.repository.DomanRepository
import co.edu.uniautonoma.inclusivereadingar.data.repository.StudentContentRepository
import co.edu.uniautonoma.inclusivereadingar.data.repository.TeacherContentRepository

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

    val arAssetRepository: BackendArAssetRepository by lazy {
        BackendArAssetRepository(
            sessionStore = sessionStore,
            assetsApi = HttpArAssetsApi(httpClient)
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

    val teacherContentRepository: TeacherContentRepository by lazy {
        TeacherContentRepository(
            sessionStore = sessionStore,
            teacherApi = HttpTeacherApi(httpClient)
        )
    }

    val domanRepository: DomanRepository by lazy {
        DomanRepository(
            sessionStore = sessionStore,
            plansApi = HttpDomanPlansApi(httpClient),
            sessionsApi = HttpDomanSessionsApi(httpClient)
        )
    }

    val docenteProgressRepository: DocenteProgressRepository by lazy {
        DocenteProgressRepository(
            sessionStore = sessionStore,
            docenteApi = HttpDocenteApi(httpClient)
        )
    }
}
