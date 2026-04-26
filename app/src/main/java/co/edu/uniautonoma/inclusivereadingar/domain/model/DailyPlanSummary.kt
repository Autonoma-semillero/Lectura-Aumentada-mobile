package co.edu.uniautonoma.inclusivereadingar.domain.model

data class DailyPlanSummary(
    val planId: String,
    val studentId: String,
    val categoryId: String,
    val targetCardsCount: Int,
    val targetSessionsCount: Int,
    val cardsCount: Int,
    val sessionsCount: Int,
    val pendingSessionsCount: Int,
    val completedSessionsCount: Int,
    val nextSessionId: String?,
    val words: List<String>
)
