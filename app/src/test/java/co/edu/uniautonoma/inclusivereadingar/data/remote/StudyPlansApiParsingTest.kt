package co.edu.uniautonoma.inclusivereadingar.data.remote

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

class StudyPlansApiParsingTest {
    @Test
    fun parseActiveStudyPlan_fullPayload_mapsPlanAndCategories() {
        val plan = parseActiveStudyPlan(
            JSONObject(
                """
                {
                  "plan_id": "p1",
                  "plan_name": "Plan lectura",
                  "level_id": "l1",
                  "level_name": "Nivel 1",
                  "date": "2026-09-21",
                  "categories": [
                    {
                      "id": "c1",
                      "name": "Animales",
                      "slug": "animales",
                      "description": "Categoría de animales",
                      "icon": "paw",
                      "available_word_cards_count": 12
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        assertThat(plan).isNotNull()
        assertThat(plan!!.planId).isEqualTo("p1")
        assertThat(plan.planName).isEqualTo("Plan lectura")
        assertThat(plan.levelId).isEqualTo("l1")
        assertThat(plan.levelName).isEqualTo("Nivel 1")
        assertThat(plan.date).isEqualTo("2026-09-21")
        assertThat(plan.categories).hasSize(1)
        assertThat(plan.categories.first().id).isEqualTo("c1")
        assertThat(plan.categories.first().availableWordCardsCount).isEqualTo(12)
    }

    @Test
    fun parseActiveStudyPlan_levelIdNull_returnsPlanWithNoCategories() {
        val plan = parseActiveStudyPlan(
            JSONObject(
                """
                {
                  "plan_id": "p1",
                  "plan_name": "Plan lectura",
                  "level_id": null,
                  "categories": []
                }
                """.trimIndent()
            )
        )

        assertThat(plan).isNotNull()
        assertThat(plan!!.planId).isEqualTo("p1")
        assertThat(plan.levelId).isNull()
        assertThat(plan.categories).isEmpty()
    }

    @Test
    fun parseActiveStudyPlan_planIdNull_returnsNull() {
        val plan = parseActiveStudyPlan(
            JSONObject(
                """
                {
                  "plan_id": null,
                  "level_id": null,
                  "categories": []
                }
                """.trimIndent()
            )
        )

        assertThat(plan).isNull()
    }
}
