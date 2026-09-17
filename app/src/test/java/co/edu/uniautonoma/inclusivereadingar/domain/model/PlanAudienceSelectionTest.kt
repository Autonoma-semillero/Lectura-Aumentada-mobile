package co.edu.uniautonoma.inclusivereadingar.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlanAudienceSelectionTest {
    @Test
    fun resolvedStudentIds_deduplicatesGroupsAndDirectStudents() {
        val groups = listOf(
            AudienceGroup("group:g1", "g1", "Primero", null, "t1", "active", listOf("s1", "s2")),
            AudienceGroup("group:g2", "g2", "Lectura", null, "t1", "active", listOf("s2", "s3"))
        )
        val selection = PlanAudienceSelection(
            groupIds = setOf("g1", "g2"),
            studentIds = setOf("s2", "s4")
        )

        assertThat(selection.resolvedStudentIds(groups))
            .containsExactly("s1", "s2", "s3", "s4")
    }

    @Test
    fun resolvedStudentIds_ignoresGroupsThatAreNotSelected() {
        val groups = listOf(
            AudienceGroup("group:g1", "g1", "Primero", null, "t1", "active", listOf("s1")),
            AudienceGroup("group:g2", "g2", "Segundo", null, "t1", "active", listOf("s2"))
        )

        assertThat(
            PlanAudienceSelection(groupIds = setOf("g2"), studentIds = setOf("s3"))
                .resolvedStudentIds(groups)
        ).containsExactly("s2", "s3")
    }
}
