package co.edu.uniautonoma.inclusivereadingar.domain.model

data class DomanSession(
    val sessionId: String,
    val studentId: String,
    val planId: String,
    val categoryId: String,
    val sessionIndex: Int,
    val status: String,
    val displayMs: Int,
    val audioMode: String,
    val mode: String,
    val cards: List<DomanSessionCard>
)
