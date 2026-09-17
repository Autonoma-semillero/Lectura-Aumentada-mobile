package co.edu.uniautonoma.inclusivereadingar.domain.model

data class OngoingDomanSession(
    val sessionId: String,
    val categoryId: String,
    val categoryName: String,
    val currentCardIndex: Int
)
