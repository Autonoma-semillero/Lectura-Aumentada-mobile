package co.edu.uniautonoma.inclusivereadingar.data.repository

import android.os.Build
import co.edu.uniautonoma.inclusivereadingar.data.local.SessionStore
import co.edu.uniautonoma.inclusivereadingar.data.remote.CategoriesApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.ProgressApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.WordCardsApi
import co.edu.uniautonoma.inclusivereadingar.domain.model.CategorySummary
import co.edu.uniautonoma.inclusivereadingar.domain.model.WordCard

interface StudentContentDataSource {
    suspend fun getAvailableCategories(): List<CategorySummary>
    suspend fun getWordCards(categoryId: String): List<WordCard>
    suspend fun registerWordViewed(card: WordCard, categoryName: String)
    suspend fun registerWordCompleted(card: WordCard, categoryName: String)
}

class StudentContentRepository(
    private val sessionStore: SessionStore,
    private val categoriesApi: CategoriesApi,
    private val wordCardsApi: WordCardsApi,
    private val progressApi: ProgressApi
) : StudentContentDataSource {
    override suspend fun getAvailableCategories(): List<CategorySummary> {
        val session = requireSession()
        return categoriesApi.getAvailableCategories(
            studentId = session.user.id,
            accessToken = session.accessToken
        )
    }

    override suspend fun getWordCards(categoryId: String): List<WordCard> {
        val session = requireSession()
        return wordCardsApi.getWordCards(
            studentId = session.user.id,
            categoryId = categoryId,
            accessToken = session.accessToken
        )
    }

    override suspend fun registerWordViewed(card: WordCard, categoryName: String) {
        val session = requireSession()
        progressApi.createProgress(
            userId = session.user.id,
            action = "word_viewed",
            learningUnitId = card.learningUnitId,
            device = deviceName(),
            payload = mapOf(
                "wordCardId" to card.id,
                "word" to card.word,
                "category" to categoryName
            ),
            accessToken = session.accessToken
        )
    }

    override suspend fun registerWordCompleted(card: WordCard, categoryName: String) {
        val session = requireSession()
        progressApi.createProgress(
            userId = session.user.id,
            action = "word_completed",
            learningUnitId = card.learningUnitId,
            device = deviceName(),
            payload = mapOf(
                "wordCardId" to card.id,
                "word" to card.word,
                "category" to categoryName
            ),
            accessToken = session.accessToken
        )
    }

    private suspend fun requireSession() = requireNotNull(sessionStore.getSession()) {
        "A valid session is required"
    }

    private fun deviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
}
