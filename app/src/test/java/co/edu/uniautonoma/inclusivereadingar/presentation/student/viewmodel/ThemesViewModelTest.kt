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
class ThemesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadThemes_emitsCategories() = runTest {
        val viewModel = ThemesViewModel(
            studentContentRepository = FakeStudentContentDataSource(
                categories = listOf(
                    CategorySummary(
                        id = "cat-1",
                        name = "Cocina",
                        slug = "cocina",
                        description = null,
                        availableWordCardsCount = 4
                    )
                )
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
        assertThat(state.categories).hasSize(1)
        assertThat(state.categories.first().name).isEqualTo("Cocina")
    }
}

private class FakeStudentContentDataSource(
    private val categories: List<CategorySummary> = emptyList(),
    private val cards: List<WordCard> = emptyList()
) : StudentContentDataSource {
    override suspend fun getAvailableCategories(): List<CategorySummary> = categories

    override suspend fun getWordCards(categoryId: String): List<WordCard> = cards

    override suspend fun registerWordViewed(card: WordCard, categoryName: String) = Unit

    override suspend fun registerWordCompleted(card: WordCard, categoryName: String) = Unit
}
