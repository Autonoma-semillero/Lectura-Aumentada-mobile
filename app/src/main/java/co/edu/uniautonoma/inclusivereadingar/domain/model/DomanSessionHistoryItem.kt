package co.edu.uniautonoma.inclusivereadingar.domain.model

data class DomanSessionHistoryItem(
    val sessionId: String,
    val dailyPlanId: String,
    val categoryId: String,
    val sessionIndex: Int,
    val status: String,
    val displayMs: Int,
    val startedAt: String?,
    val completedAt: String?
)
