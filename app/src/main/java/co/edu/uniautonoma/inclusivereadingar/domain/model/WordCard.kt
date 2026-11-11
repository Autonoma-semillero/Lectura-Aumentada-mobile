package co.edu.uniautonoma.inclusivereadingar.domain.model

data class WordCard(
    val id: String,
    // Per-student copies carry their owner; catalog cards (teacher view) have none.
    val studentId: String? = null,
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
