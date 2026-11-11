package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import co.edu.uniautonoma.inclusivereadingar.MainDispatcherRule
import co.edu.uniautonoma.inclusivereadingar.domain.model.Category
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateWordCardViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val animals = Category("cat-1", "Animales", "animales", null, null, sortOrder = 2, wordCardsCount = 0)
    private val colors = Category("cat-2", "Colores", "colores", null, null, sortOrder = 1, wordCardsCount = 0)

    @Test
    fun init_loadsCategoriesSortedByOrder_withoutLoadingStudents() = runTest {
        val repository = FakeTeacherContentDataSource(categories = listOf(animals, colors))

        val viewModel = CreateWordCardViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoadingCategories).isFalse()
        assertThat(state.isLoadingArModels).isFalse()
        assertThat(state.categories.map { it.id }).containsExactly("cat-2", "cat-1").inOrder()
        assertThat(repository.studentsRequested).isFalse()
    }

    @Test
    fun canSave_needsOnlyAWordAndACategory() = runTest {
        val viewModel = CreateWordCardViewModel(FakeTeacherContentDataSource(categories = listOf(animals)))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.canSave).isFalse()

        viewModel.updateWord("gato")
        assertThat(viewModel.uiState.value.canSave).isFalse()

        viewModel.selectCategory("cat-1")
        assertThat(viewModel.uiState.value.canSave).isTrue()
    }

    @Test
    fun save_withoutWordOrCategory_showsTheValidationMessageAndDoesNotCreate() = runTest {
        val repository = FakeTeacherContentDataSource(categories = listOf(animals))
        val viewModel = CreateWordCardViewModel(repository)
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("Completa la palabra y la temática.")
        assertThat(repository.created).isEmpty()
        assertThat(viewModel.uiState.value.saved).isFalse()
    }

    @Test
    fun save_createsTheCatalogCard_trimmedAndWithoutAStudent() = runTest {
        val repository = FakeTeacherContentDataSource(categories = listOf(animals))
        val viewModel = CreateWordCardViewModel(repository)
        advanceUntilIdle()

        viewModel.updateWord("  gato ")
        viewModel.selectCategory("cat-1")
        viewModel.updateAudioUrl(" https://cdn.test/gato.mp3 ")
        viewModel.save()
        advanceUntilIdle()

        assertThat(repository.created).containsExactly(
            FakeTeacherContentDataSource.CreatedCard(
                word = "gato",
                categoryId = "cat-1",
                audioUrl = "https://cdn.test/gato.mp3",
                learningUnitId = null
            )
        )
        assertThat(viewModel.uiState.value.saved).isTrue()
        assertThat(viewModel.uiState.value.isSaving).isFalse()
    }

    @Test
    fun updateWord_isLimitedToFifteenCharacters() = runTest {
        val viewModel = CreateWordCardViewModel(FakeTeacherContentDataSource())
        advanceUntilIdle()

        viewModel.updateWord("a".repeat(30))

        assertThat(viewModel.uiState.value.word).hasLength(15)
    }

    @Test
    fun save_failure_keepsTheFormOpenAndStopsSaving() = runTest {
        val repository = FakeTeacherContentDataSource(
            categories = listOf(animals),
            failCreateWith = IllegalStateException("duplicada")
        )
        val viewModel = CreateWordCardViewModel(repository)
        advanceUntilIdle()
        viewModel.updateWord("gato")
        viewModel.selectCategory("cat-1")

        viewModel.save()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.saved).isFalse()
        assertThat(viewModel.uiState.value.isSaving).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isNotNull()
    }
}
