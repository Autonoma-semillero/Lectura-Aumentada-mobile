package co.edu.uniautonoma.inclusivereadingar.data.repository

import co.edu.uniautonoma.inclusivereadingar.data.local.SessionStore
import co.edu.uniautonoma.inclusivereadingar.data.remote.DocenteApi
import co.edu.uniautonoma.inclusivereadingar.domain.model.AppUser
import co.edu.uniautonoma.inclusivereadingar.domain.model.AuthSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.CompletedCardsPage
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentCategoryProgress

interface DocenteProgressDataSource {
    suspend fun getStudents(): List<AppUser>
    suspend fun getStudentCategoryProgress(studentId: String): List<StudentCategoryProgress>
    suspend fun getStudentCompletedCards(
        studentId: String,
        categoryId: String?,
        page: Int,
        limit: Int
    ): CompletedCardsPage
}

class DocenteProgressRepository(
    private val sessionStore: SessionStore,
    private val docenteApi: DocenteApi
) : DocenteProgressDataSource {

    override suspend fun getStudents(): List<AppUser> {
        val session = requireSession()
        return docenteApi.getStudents(session.accessToken)
    }

    override suspend fun getStudentCategoryProgress(studentId: String): List<StudentCategoryProgress> {
        val session = requireSession()
        return docenteApi.getStudentCategoryProgress(studentId, session.accessToken)
    }

    override suspend fun getStudentCompletedCards(
        studentId: String,
        categoryId: String?,
        page: Int,
        limit: Int
    ): CompletedCardsPage {
        val session = requireSession()
        return docenteApi.getStudentCompletedCards(studentId, categoryId, page, limit, session.accessToken)
    }

    private suspend fun requireSession(): AuthSession = requireNotNull(sessionStore.getSession()) {
        "A valid session is required"
    }
}
