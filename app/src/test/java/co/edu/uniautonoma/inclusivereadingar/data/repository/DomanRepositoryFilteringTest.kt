package co.edu.uniautonoma.inclusivereadingar.data.repository

import co.edu.uniautonoma.inclusivereadingar.domain.model.ActiveStudyPlan
import co.edu.uniautonoma.inclusivereadingar.domain.model.ActiveStudyPlanCategory
import co.edu.uniautonoma.inclusivereadingar.domain.model.DailyPlanSummary
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DomanRepositoryFilteringTest {
    @Test
    fun filterAndEnrichTodayActivities_keepsOnlyCurrentPlanLevelAndCategories() {
        val activePlan = activePlan()
        val current = dailyPlan(
            id = "daily-current",
            studyPlanId = "STUDY-1",
            levelId = "LEVEL-1",
            categoryId = "CATEGORY-1"
        )
        val legacy = dailyPlan(
            id = "daily-legacy",
            studyPlanId = null,
            levelId = null,
            categoryId = "category-1"
        )
        val obsoletePlan = dailyPlan(
            id = "daily-old-plan",
            studyPlanId = "study-old",
            levelId = "level-1",
            categoryId = "category-1"
        )
        val obsoleteLevel = dailyPlan(
            id = "daily-old-level",
            studyPlanId = "study-1",
            levelId = "level-old",
            categoryId = "category-1"
        )
        val categoryOutsideLevel = dailyPlan(
            id = "daily-other-category",
            studyPlanId = "study-1",
            levelId = "level-1",
            categoryId = "category-2"
        )

        val result = filterAndEnrichTodayActivities(
            listOf(current, legacy, obsoletePlan, obsoleteLevel, categoryOutsideLevel),
            activePlan
        )

        assertThat(result.map { it.planId }).containsExactly("daily-current")
        assertThat(result.single().categoryName).isEqualTo("Animales")
        assertThat(result.single().categoryId).isEqualTo("CATEGORY-1")
    }

    @Test
    fun missingActiveCategories_doesNotTreatAnOldPlanAsTodaysActivity() {
        val activePlan = activePlan()
        val oldPlanActivity = dailyPlan(
            id = "daily-old-plan",
            studyPlanId = "study-old",
            levelId = "level-1",
            categoryId = "category-1"
        )

        val result = missingActiveCategories(listOf(oldPlanActivity), activePlan)

        assertThat(result.map { it.id }).containsExactly("category-1")
    }

    @Test
    fun missingActiveCategories_skipsCategoriesAlreadyCreatedForTheActivePlan() {
        val activePlan = activePlan()
        val currentActivity = dailyPlan(
            id = "daily-current",
            studyPlanId = "STUDY-1",
            levelId = "LEVEL-1",
            categoryId = "CATEGORY-1"
        )

        val result = missingActiveCategories(listOf(currentActivity), activePlan)

        assertThat(result).isEmpty()
    }

    private fun activePlan() = ActiveStudyPlan(
        planId = "study-1",
        planName = "Lectura inicial",
        levelId = "level-1",
        levelName = "Nivel uno",
        date = "2026-09-22",
        categories = listOf(
            ActiveStudyPlanCategory(
                id = "category-1",
                name = "Animales",
                slug = "animales",
                description = null,
                icon = null,
                availableWordCardsCount = 5
            )
        )
    )

    private fun dailyPlan(
        id: String,
        studyPlanId: String?,
        levelId: String?,
        categoryId: String
    ) = DailyPlanSummary(
        planId = id,
        studentId = "student-1",
        categoryId = categoryId,
        studyPlanId = studyPlanId,
        studyPlanLevelId = levelId,
        targetCardsCount = 5,
        targetSessionsCount = 3,
        cardsCount = 5,
        sessionsCount = 3,
        pendingSessionsCount = 3,
        completedSessionsCount = 0,
        nextSessionId = null,
        words = emptyList()
    )
}
