package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import co.edu.uniautonoma.inclusivereadingar.data.repository.TeacherContentDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.AppUser
import co.edu.uniautonoma.inclusivereadingar.domain.model.ArModelOption
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudioUploadInput
import co.edu.uniautonoma.inclusivereadingar.domain.model.Category
import co.edu.uniautonoma.inclusivereadingar.domain.model.UploadedAudio
import co.edu.uniautonoma.inclusivereadingar.domain.model.WordCard

/** In-memory stand-in for the teacher repository; only the card operations are meaningful. */
internal class FakeTeacherContentDataSource(
    var cards: List<WordCard> = emptyList(),
    var categories: List<Category> = emptyList(),
    var arModels: List<ArModelOption> = emptyList(),
    var failCardsWith: Throwable? = null,
    var failArchiveWith: Throwable? = null,
    var failCreateWith: Throwable? = null
) : TeacherContentDataSource {
    data class CreatedCard(
        val word: String,
        val categoryId: String,
        val audioUrl: String?,
        val learningUnitId: String?
    )

    val created = mutableListOf<CreatedCard>()
    val archivedIds = mutableListOf<String>()
    var cardsRequestedFor: String? = null
    var studentsRequested = false

    override suspend fun getCategories(): List<Category> = categories

    override suspend fun createCategory(
        name: String,
        slug: String,
        description: String?,
        icon: String?,
        sortOrder: Int
    ): Category = throw UnsupportedOperationException()

    override suspend fun updateCategory(
        id: String,
        name: String,
        slug: String,
        description: String?,
        icon: String?,
        sortOrder: Int
    ): Category = throw UnsupportedOperationException()

    override suspend fun deleteCategory(id: String) = throw UnsupportedOperationException()

    override suspend fun getStudents(): List<AppUser> {
        studentsRequested = true
        return emptyList()
    }

    override suspend fun getWordCardsForCategory(categoryId: String): List<WordCard> {
        cardsRequestedFor = categoryId
        failCardsWith?.let { throw it }
        return cards
    }

    override suspend fun getWordCardById(id: String): WordCard =
        cards.first { it.id == id }

    override suspend fun getArModels(): List<ArModelOption> = arModels

    override suspend fun createWordCard(
        word: String,
        categoryId: String,
        audioUrl: String?,
        learningUnitId: String?
    ): WordCard {
        failCreateWith?.let { throw it }
        created += CreatedCard(word, categoryId, audioUrl, learningUnitId)
        return card(id = "new-${created.size}", word = word, categoryId = categoryId)
    }

    override suspend fun updateWordCard(
        id: String,
        word: String,
        audioUrl: String?,
        learningUnitId: String?
    ): WordCard = throw UnsupportedOperationException()

    override suspend fun associateArContent(
        learningUnitId: String,
        markerId: String,
        model3dUrl: String
    ): ArModelOption = ArModelOption(learningUnitId, markerId, word = "", model3dUrl = model3dUrl)

    override suspend fun archiveWordCard(id: String) {
        failArchiveWith?.let { throw it }
        archivedIds += id
    }

    override suspend fun uploadAudio(input: AudioUploadInput): UploadedAudio =
        throw UnsupportedOperationException()
}

internal fun card(
    id: String,
    word: String,
    status: String = "active",
    categoryId: String? = "cat-1"
) = WordCard(
    id = id,
    word = word,
    status = status,
    categoryId = categoryId,
    learningUnitId = null,
    audioUrl = null,
    language = null,
    initialLetter = word.take(1).uppercase(),
    timesShown = 0,
    timesAudioPlayed = null
)
