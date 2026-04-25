package co.edu.uniautonoma.inclusivereadingar.domain.model

data class WordCard(
    val id: String,
    val studentId: String,
    val word: String,
    val status: String,
    val categoryId: String?,
    val learningUnitId: String?,
    val audioUrl: String?,
    val language: String?,
    val initialLetter: String,
    val timesShown: Int,
    val timesAudioPlayed: Int?
)
