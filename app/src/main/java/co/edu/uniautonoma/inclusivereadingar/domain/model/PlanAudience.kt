package co.edu.uniautonoma.inclusivereadingar.domain.model

data class PlanAudienceSelection(
    val groupIds: Set<String> = emptySet(),
    val studentIds: Set<String> = emptySet()
) {
    fun resolvedStudentIds(groups: Collection<AudienceGroup>): Set<String> {
        val studentsFromGroups = groups
            .asSequence()
            .filter { it.id in groupIds }
            .flatMap { it.studentIds.asSequence() }
            .toSet()
        return studentsFromGroups + studentIds
    }
}

data class BulkPlanAudienceStudent(
    val studentId: String,
    val sources: List<String>
)

data class BulkPlanAudience(
    val studentIds: List<String>,
    val students: List<BulkPlanAudienceStudent>
)

enum class BulkPlanStatus {
    GENERATED,
    EXISTING,
    FAILED;

    companion object {
        fun fromApi(value: String): BulkPlanStatus = when (value.lowercase()) {
            "generated" -> GENERATED
            "existing" -> EXISTING
            else -> FAILED
        }
    }
}

data class BulkPlanSummary(
    val total: Int,
    val generated: Int,
    val existing: Int,
    val failed: Int
)

data class BulkPlanItemResult(
    val studentId: String,
    val status: BulkPlanStatus,
    val plan: DailyPlanSummary?,
    val error: String?,
    val sources: List<String>
)

data class BulkPlanGenerationResult(
    val audience: BulkPlanAudience,
    val summary: BulkPlanSummary,
    val results: List<BulkPlanItemResult>
)
