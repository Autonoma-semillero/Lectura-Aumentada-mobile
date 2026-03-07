package co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel

import co.edu.uniautonoma.inclusivereadingar.MainDispatcherRule
import co.edu.uniautonoma.inclusivereadingar.data.repository.MockLearningRepository
import co.edu.uniautonoma.inclusivereadingar.domain.usecase.GetDailyWordsUseCase
import co.edu.uniautonoma.inclusivereadingar.domain.usecase.RegisterProgressUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebArViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsWordsSuccessfully() = runTest {
        val repository = MockLearningRepository()
        val viewModel = WebArViewModel(GetDailyWordsUseCase(repository), RegisterProgressUseCase(repository))

        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertThat(state.isLoading).isFalse()
        assertThat(state.units).hasSize(3)
        assertThat(state.currentWordIndex).isEqualTo(0)
        assertThat(state.generalError).isNull()
    }

    @Test
    fun onWebError_updatesErrorState() = runTest {
        val repository = MockLearningRepository()
        val viewModel = WebArViewModel(GetDailyWordsUseCase(repository), RegisterProgressUseCase(repository))

        viewModel.onWebError("Sin conectividad")

        assertThat(viewModel.uiState.value.webError).isEqualTo("Sin conectividad")
    }

    @Test
    fun nextAndPreviousWord_updatesIndexWithinBounds() = runTest {
        val repository = MockLearningRepository()
        val viewModel = WebArViewModel(GetDailyWordsUseCase(repository), RegisterProgressUseCase(repository))

        advanceUntilIdle()
        viewModel.nextWord()
        viewModel.nextWord()
        viewModel.nextWord()

        assertThat(viewModel.uiState.value.currentWordIndex).isEqualTo(2)

        viewModel.previousWord()
        assertThat(viewModel.uiState.value.currentWordIndex).isEqualTo(1)
    }

    @Test
    fun registerSimulatedRead_tracksPracticeAndAdvancesWord() = runTest {
        val repository = MockLearningRepository()
        val viewModel = WebArViewModel(GetDailyWordsUseCase(repository), RegisterProgressUseCase(repository))

        advanceUntilIdle()
        viewModel.registerSimulatedRead()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.lastRegisteredWord).isEqualTo("CASA")
        assertThat(state.completedPractices).isEqualTo(1)
        assertThat(state.currentWordIndex).isEqualTo(1)
    }
}
