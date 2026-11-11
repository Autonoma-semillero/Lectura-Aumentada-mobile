package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import co.edu.uniautonoma.inclusivereadingar.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryCardsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_listsActiveCardsSortedByWord_andHidesArchivedOnes() = runTest {
        val repository = FakeTeacherContentDataSource(
            cards = listOf(
                card("1", "pato"),
                card("2", "caballo"),
                card("3", "viejo", status = "archived"),
                card("4", "gato")
            )
        )
        val viewModel = CategoryCardsViewModel(repository)

        viewModel.load("cat-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.cards.map { it.word }).containsExactly("caballo", "gato", "pato").inOrder()
        assertThat(repository.cardsRequestedFor).isEqualTo("cat-1")
    }

    @Test
    fun load_doesNotRequestStudents_becauseCatalogCardsHaveNoOwner() = runTest {
        val repository = FakeTeacherContentDataSource(cards = listOf(card("1", "gato")))
        val viewModel = CategoryCardsViewModel(repository)

        viewModel.load("cat-1")
        advanceUntilIdle()

        assertThat(repository.studentsRequested).isFalse()
    }

    @Test
    fun load_failure_exposesTheErrorAndStopsLoading() = runTest {
        val repository = FakeTeacherContentDataSource(failCardsWith = IllegalStateException("sin red"))
        val viewModel = CategoryCardsViewModel(repository)

        viewModel.load("cat-1")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("sin red")

        viewModel.clearError()
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    @Test
    fun confirmArchive_archivesTheCardAndRemovesItFromTheList() = runTest {
        val repository = FakeTeacherContentDataSource(cards = listOf(card("1", "gato"), card("2", "pato")))
        val viewModel = CategoryCardsViewModel(repository)
        viewModel.load("cat-1")
        advanceUntilIdle()

        viewModel.requestArchive("1")
        assertThat(viewModel.uiState.value.archiveConfirmCardId).isEqualTo("1")
        viewModel.confirmArchive()
        advanceUntilIdle()

        assertThat(repository.archivedIds).containsExactly("1")
        assertThat(viewModel.uiState.value.cards.map { it.id }).containsExactly("2")
        assertThat(viewModel.uiState.value.isArchiving).isFalse()
        assertThat(viewModel.uiState.value.archiveConfirmCardId).isNull()
    }

    @Test
    fun cancelArchive_leavesTheListUntouched() = runTest {
        val repository = FakeTeacherContentDataSource(cards = listOf(card("1", "gato")))
        val viewModel = CategoryCardsViewModel(repository)
        viewModel.load("cat-1")
        advanceUntilIdle()

        viewModel.requestArchive("1")
        viewModel.cancelArchive()
        viewModel.confirmArchive()
        advanceUntilIdle()

        assertThat(repository.archivedIds).isEmpty()
        assertThat(viewModel.uiState.value.cards).hasSize(1)
    }

    @Test
    fun confirmArchive_failure_keepsTheCardAndShowsTheError() = runTest {
        val repository = FakeTeacherContentDataSource(
            cards = listOf(card("1", "gato")),
            failArchiveWith = IllegalStateException("no se pudo")
        )
        val viewModel = CategoryCardsViewModel(repository)
        viewModel.load("cat-1")
        advanceUntilIdle()

        viewModel.requestArchive("1")
        viewModel.confirmArchive()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.cards).hasSize(1)
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("no se pudo")
        assertThat(viewModel.uiState.value.isArchiving).isFalse()
    }
}
