package co.edu.uniautonoma.inclusivereadingar.domain.model

data class StudentProgressSummary(
    val studentId: String,
    val plannedSessionsCount: Int,
    val inProgressSessionsCount: Int,
    val completedSessionsCount: Int,
    val cardsNewCount: Int,
    val cardsActiveCount: Int,
    val cardsCompletedCount: Int,
    val lastActivityAt: String?
)
