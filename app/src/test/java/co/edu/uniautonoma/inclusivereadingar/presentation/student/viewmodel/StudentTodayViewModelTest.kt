package co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel

import co.edu.uniautonoma.inclusivereadingar.MainDispatcherRule
import co.edu.uniautonoma.inclusivereadingar.data.remote.BackendException
import co.edu.uniautonoma.inclusivereadingar.data.repository.StudentTodayDataSource
import co.edu.uniautonoma.inclusivereadingar.data.repository.TodayActivitiesResult
import co.edu.uniautonoma.inclusivereadingar.domain.model.ActiveStudyPlan
import co.edu.uniautonoma.inclusivereadingar.domain.model.ActiveStudyPlanCategory
import co.edu.uniautonoma.inclusivereadingar.domain.model.DailyPlanSummary
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudentTodayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_activePlanWithDueCards_emitsDueToday() = runTest {
        val dataSource = FakeStudentTodayDataSource(
            plan = plan(),
            activities = TodayActivitiesResult(
                activities = listOf(dailyPlan(pendingSessionsCount = 3)),
                countsLoadFailed = false
            )
        )
        val viewModel = StudentTodayViewModel(dataSource)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(StudentTodayUiState.DueToday::class.java)
        state as StudentTodayUiState.DueToday
        assertThat(state.planName).isEqualTo("Plan Nivel 1")
        assertThat(state.activities).hasSize(1)
        assertThat(state.activities.first().pendingSessionsCount).isEqualTo(3)
        assertThat(state.activities.first().categoryName).isEqualTo("Cocina")
        assertThat(dataSource.requestedPlan).isEqualTo(plan())
    }

    @Test
    fun load_noActivePlan_emitsNoActivePlan() = runTest {
        val dataSource = FakeStudentTodayDataSource(
            plan = null,
            activities = TodayActivitiesResult(activities = emptyList(), countsLoadFailed = false)
        )
        val viewModel = StudentTodayViewModel(dataSource)

        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(StudentTodayUiState.NoActivePlan)
        assertThat(dataSource.activitiesRequests).isEqualTo(0)
    }

    @Test
    fun load_activePlanZeroDueCards_emitsUpToDate() = runTest {
        val dataSource = FakeStudentTodayDataSource(
            plan = plan(),
            activities = TodayActivitiesResult(
                activities = listOf(dailyPlan(pendingSessionsCount = 0)),
                countsLoadFailed = false
            )
        )
        val viewModel = StudentTodayViewModel(dataSource)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(StudentTodayUiState.UpToDate::class.java)
        assertThat((state as StudentTodayUiState.UpToDate).planName).isEqualTo("Plan Nivel 1")
    }

    @Test
    fun load_completedCategory_keepsNextPendingCategoryAsPrimaryActivity() = runTest {
        val dataSource = FakeStudentTodayDataSource(
            plan = plan(),
            activities = TodayActivitiesResult(
                activities = listOf(
                    dailyPlan(pendingSessionsCount = 0, categoryId = "cat-1", categoryName = "Animales"),
                    dailyPlan(pendingSessionsCount = 3, categoryId = "cat-2", categoryName = "Cocina")
                ),
                countsLoadFailed = false
            )
        )
        val viewModel = StudentTodayViewModel(dataSource)

        advanceUntilIdle()

        val state = viewModel.uiState.value as StudentTodayUiState.DueToday
        assertThat(state.activities).hasSize(2)
        assertThat(state.activities.first().categoryName).isEqualTo("Cocina")
        assertThat(state.activities.last().categoryName).isEqualTo("Animales")
    }

    @Test
    fun load_countsFetchFails_emitsErrorNotUpToDate() = runTest {
        val dataSource = FakeStudentTodayDataSource(
            plan = plan(),
            activities = TodayActivitiesResult(
                activities = listOf(dailyPlan(pendingSessionsCount = 0)),
                countsLoadFailed = true
            )
        )
        val viewModel = StudentTodayViewModel(dataSource)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(StudentTodayUiState.Error::class.java)
    }

    @Test
    fun load_planFetchThrows_emitsChildFriendlyError() = runTest {
        val dataSource = FakeStudentTodayDataSource(
            planFailure = BackendException(503, "El servicio no está disponible.")
        )
        val viewModel = StudentTodayViewModel(dataSource)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(StudentTodayUiState.Error::class.java)
        assertThat((state as StudentTodayUiState.Error).message)
            .isEqualTo("No pudimos abrir tu lectura. Probemos otra vez.")
    }

    @Test
    fun load_startsInLoadingState() = runTest {
        val dataSource = FakeStudentTodayDataSource(
            plan = plan(),
            activities = TodayActivitiesResult(activities = emptyList(), countsLoadFailed = false)
        )
        val viewModel = StudentTodayViewModel(dataSource)

        assertThat(viewModel.uiState.value).isEqualTo(StudentTodayUiState.Loading)
    }

    private fun plan(): ActiveStudyPlan = ActiveStudyPlan(
        planId = "plan-1",
        planName = "Plan Nivel 1",
        levelId = "level-1",
        levelName = "Nivel 1",
        date = "2026-09-21",
        categories = listOf(
            ActiveStudyPlanCategory(
                id = "cat-1",
                name = "Cocina",
                slug = "cocina",
                description = null,
                icon = null,
                availableWordCardsCount = 5
            )
        )
    )

    private fun dailyPlan(
        pendingSessionsCount: Int,
        categoryId: String = "cat-1",
        categoryName: String = "Cocina"
    ): DailyPlanSummary = DailyPlanSummary(
        planId = "plan-1",
        studentId = "student-1",
        categoryId = categoryId,
        studyPlanId = "plan-1",
        studyPlanLevelId = "level-1",
        categoryName = categoryName,
        targetCardsCount = 5,
        targetSessionsCount = 1,
        cardsCount = 5,
        sessionsCount = 1,
        pendingSessionsCount = pendingSessionsCount,
        completedSessionsCount = 1 - pendingSessionsCount,
        nextSessionId = null,
        words = listOf("CASA")
    )
}

private class FakeStudentTodayDataSource(
    private val plan: ActiveStudyPlan? = null,
    private val activities: TodayActivitiesResult = TodayActivitiesResult(emptyList(), false),
    private val planFailure: Throwable? = null,
    private val activitiesFailure: Throwable? = null
) : StudentTodayDataSource {
    var activitiesRequests: Int = 0
        private set
    var requestedPlan: ActiveStudyPlan? = null
        private set

    override suspend fun getActivePlanForCurrentStudent(): ActiveStudyPlan? {
        planFailure?.let { throw it }
        return plan
    }

    override suspend fun getTodayActivitiesForCurrentStudent(
        activePlan: ActiveStudyPlan
    ): TodayActivitiesResult {
        activitiesRequests++
        requestedPlan = activePlan
        activitiesFailure?.let { throw it }
        return activities
    }
}
