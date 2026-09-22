package co.edu.uniautonoma.inclusivereadingar.data.remote

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

class DomanPlansApiParsingTest {
    @Test
    fun parseDailyPlanListItem_keepsStudyPlanReferences() {
        val plan = parseDailyPlanListItem(
            JSONObject(
                """
                {
                  "id": "daily-1",
                  "student_id": "student-1",
                  "category_id": "category-1",
                  "study_plan_id": "study-1",
                  "study_plan_level_id": "level-1",
                  "target_cards_count": 5,
                  "target_sessions_count": 3
                }
                """.trimIndent()
            )
        )

        assertThat(plan.studyPlanId).isEqualTo("study-1")
        assertThat(plan.studyPlanLevelId).isEqualTo("level-1")
        assertThat(plan.categoryName).isNull()
    }

    @Test
    fun parseDailyPlanSummary_keepsStudyPlanReferencesFromNestedPlan() {
        val plan = parseDailyPlanSummary(
            JSONObject(
                """
                {
                  "plan": {
                    "id": "daily-1",
                    "student_id": "student-1",
                    "category_id": "category-1",
                    "study_plan_id": "study-1",
                    "study_plan_level_id": "level-1",
                    "target_cards_count": 2,
                    "target_sessions_count": 1
                  },
                  "cards": []
                }
                """.trimIndent()
            )
        )

        assertThat(plan.studyPlanId).isEqualTo("study-1")
        assertThat(plan.studyPlanLevelId).isEqualTo("level-1")
    }

    @Test
    fun parseDailyPlanListItem_legacyPlanLeavesStudyPlanReferencesNull() {
        val plan = parseDailyPlanListItem(
            JSONObject(
                """
                {
                  "id": "daily-legacy",
                  "student_id": "student-1",
                  "category_id": "category-1",
                  "target_cards_count": 5,
                  "target_sessions_count": 3
                }
                """.trimIndent()
            )
        )

        assertThat(plan.studyPlanId).isNull()
        assertThat(plan.studyPlanLevelId).isNull()
    }
}
