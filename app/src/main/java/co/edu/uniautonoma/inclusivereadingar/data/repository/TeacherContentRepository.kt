package co.edu.uniautonoma.inclusivereadingar.data.repository

import co.edu.uniautonoma.inclusivereadingar.data.local.SessionStore
import co.edu.uniautonoma.inclusivereadingar.data.remote.TeacherApi
import co.edu.uniautonoma.inclusivereadingar.domain.model.AppUser
import co.edu.uniautonoma.inclusivereadingar.domain.model.AuthSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.Category
import co.edu.uniautonoma.inclusivereadingar.domain.model.WordCard

interface TeacherContentDataSource {
    suspend fun getCategories(): List<Category>
    suspend fun createCategory(
        name: String,
        slug: String,
        description: String?,
        icon: String?,
        sortOrder: Int
    ): Category

    suspend fun updateCategory(
        id: String,
        name: String,
        slug: String,
        description: String?,
        icon: String?,
        sortOrder: Int
    ): Category

    suspend fun deleteCategory(id: String)
    suspend fun getStudents(): List<AppUser>
    suspend fun createWordCard(
        studentId: String,
        word: String,
        categoryId: String,
        audioUrl: String?
    ): WordCard
}

class TeacherContentRepository(
    private val sessionStore: SessionStore,
    private val teacherApi: TeacherApi
) : TeacherContentDataSource {
    override suspend fun getCategories(): List<Category> {
        val session = requireSession()
        return teacherApi.getCategories(session.accessToken)
    }

    override suspend fun createCategory(
        name: String,
        slug: String,
        description: String?,
        icon: String?,
        sortOrder: Int
    ): Category {
        val session = requireSession()
        return teacherApi.createCategory(
            name = name,
            slug = slug,
            description = description,
            icon = icon,
            sortOrder = sortOrder,
            accessToken = session.accessToken
        )
    }

    override suspend fun updateCategory(
        id: String,
        name: String,
        slug: String,
        description: String?,
        icon: String?,
        sortOrder: Int
    ): Category {
        val session = requireSession()
        return teacherApi.updateCategory(
            id = id,
            name = name,
            slug = slug,
            description = description,
            icon = icon,
            sortOrder = sortOrder,
            accessToken = session.accessToken
        )
    }

    override suspend fun deleteCategory(id: String) {
        val session = requireSession()
        teacherApi.deleteCategory(id, session.accessToken)
    }

    override suspend fun getStudents(): List<AppUser> {
        val session = requireSession()
        return teacherApi.getUsers(session.accessToken)
            .filter { user -> user.roles.contains("student") }
            .sortedBy { user -> user.displayName ?: user.email }
    }

    override suspend fun createWordCard(
        studentId: String,
        word: String,
        categoryId: String,
        audioUrl: String?
    ): WordCard {
        val session = requireSession()
        return teacherApi.createWordCard(
            studentId = studentId,
            word = word,
            categoryId = categoryId,
            audioUrl = audioUrl,
            accessToken = session.accessToken
        )
    }

    private suspend fun requireSession(): AuthSession = requireNotNull(sessionStore.getSession()) {
        "A valid session is required"
    }
}
