package co.edu.uniautonoma.inclusivereadingar.domain.model

data class CompletedCard(
    val id: String,
    val word: String,
    val audioUrl: String?,
    val categoryId: String,
    val categoryName: String,
    val timesShown: Int,
    val timesAudioPlayed: Int,
    val completedAt: String?
)
