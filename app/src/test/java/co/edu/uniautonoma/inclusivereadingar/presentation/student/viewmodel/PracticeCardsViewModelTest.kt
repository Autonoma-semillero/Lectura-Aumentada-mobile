package co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel

import co.edu.uniautonoma.inclusivereadingar.MainDispatcherRule
import co.edu.uniautonoma.inclusivereadingar.data.repository.StudentContentDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.CategorySummary
import co.edu.uniautonoma.inclusivereadingar.domain.model.WordCard
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeCardsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadAndCompleteCurrentWord_advancesSequence() = runTest {
        val viewModel = PracticeCardsViewModel(
            studentContentRepository = FakePracticeContentDataSource(
                cards = listOf(
                    WordCard(
                        id = "card-1",
                        studentId = "student-1",
                        word = "CASA",
                        status = "active",
                        categoryId = "cat-1",
                        learningUnitId = "unit-1",
                        audioUrl = null,
                        language = "es",
                        initialLetter = "C",
                        timesShown = 0,
                        timesAudioPlayed = 0
                    ),
                    WordCard(
                        id = "card-2",
                        studentId = "student-1",
                        word = "SOL",
                        status = "active",
                        categoryId = "cat-1",
                        learningUnitId = "unit-2",
                        audioUrl = null,
                        language = "es",
                        initialLetter = "S",
                        timesShown = 0,
                        timesAudioPlayed = 0
                    )
                )
            )
        )

        viewModel.load(categoryId = "cat-1", categoryName = "Cocina")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentCard?.word).isEqualTo("CASA")

        viewModel.completeCurrentWord()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.completedCount).isEqualTo(1)
        assertThat(state.currentIndex).isEqualTo(1)
        assertThat(state.currentCard?.word).isEqualTo("SOL")
    }
}

private class FakePracticeContentDataSource(
    private val cards: List<WordCard> = emptyList()
) : StudentContentDataSource {
    override suspend fun getAvailableCategories(): List<CategorySummary> = emptyList()

    override suspend fun getWordCards(categoryId: String): List<WordCard> = cards

    override suspend fun registerWordViewed(card: WordCard, categoryName: String) = Unit

    override suspend fun registerWordCompleted(card: WordCard, categoryName: String) = Unit
}
