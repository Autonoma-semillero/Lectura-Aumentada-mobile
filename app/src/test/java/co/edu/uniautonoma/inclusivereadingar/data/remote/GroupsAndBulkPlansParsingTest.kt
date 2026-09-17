package co.edu.uniautonoma.inclusivereadingar.data.remote

import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceGroup
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceStudent
import co.edu.uniautonoma.inclusivereadingar.domain.model.BulkPlanStatus
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

class GroupsAndBulkPlansParsingTest {
    @Test
    fun parseAudienceSearch_supportsStudentsGroupsAndOrphans() {
        val parsed = parseAudienceSearch(
            """
            {
              "items": [
                {
                  "type": "student",
                  "audience_key": "student:s1",
                  "id": "s1",
                  "display_name": "Ana",
                  "email": "ana@example.com",
                  "group_ids": [],
                  "unassigned": true
                },
                {
                  "type": "group",
                  "audience_key": "group:g1",
                  "id": "g1",
                  "name": "Lectores",
                  "teacher_id": "t1",
                  "status": "active",
                  "student_ids": ["s1", "s2"]
                }
              ]
            }
            """.trimIndent()
        )

        val student = parsed.items.filterIsInstance<AudienceStudent>().single()
        val group = parsed.items.filterIsInstance<AudienceGroup>().single()
        assertThat(student.label).isEqualTo("Ana")
        assertThat(student.unassigned).isTrue()
        assertThat(group.studentIds).containsExactly("s1", "s2").inOrder()
    }

    @Test
    fun parseStudentGroup_readsMembershipSnapshot() {
        val group = parseStudentGroup(
            JSONObject(
                """
                {
                  "id": "g1",
                  "name": "Lectores",
                  "normalized_name": "lectores",
                  "teacher_id": "t1",
                  "created_by": "t1",
                  "status": "active",
                  "student_ids": ["s1"]
                }
                """.trimIndent()
            )
        )

        assertThat(group.name).isEqualTo("Lectores")
        assertThat(group.studentIds).containsExactly("s1")
    }

    @Test
    fun parseBulkResult_keepsPartialFailuresAndPlans() {
        val result = parseBulkPlanGenerationResult(
            """
            {
              "audience": {
                "student_ids": ["s1", "s2"],
                "students": [
                  {"student_id": "s1", "sources": [{"type": "group", "group_id": "g1"}]},
                  {"student_id": "s2", "sources": [{"type": "direct"}]}
                ]
              },
              "summary": {"total": 2, "generated": 1, "existing": 0, "failed": 1},
              "results": [
                {
                  "student_id": "s1",
                  "status": "generated",
                  "sources": [{"type": "group", "group_id": "g1"}],
                  "plan": {
                    "id": "p1",
                    "student_id": "s1",
                    "category_id": "c1",
                    "target_cards_count": 5,
                    "target_sessions_count": 3
                  }
                },
                {
                  "student_id": "s2",
                  "status": "failed",
                  "sources": [{"type": "direct"}],
                  "error": "Sin tarjetas activas"
                }
              ]
            }
            """.trimIndent()
        )

        assertThat(result.summary.failed).isEqualTo(1)
        assertThat(result.results.first().plan?.planId).isEqualTo("p1")
        assertThat(result.results.first().sources).containsExactly("group:g1")
        assertThat(result.audience.students.last().sources).containsExactly("direct")
        assertThat(result.results.last().status).isEqualTo(BulkPlanStatus.FAILED)
        assertThat(result.results.last().error).isEqualTo("Sin tarjetas activas")
    }
}
