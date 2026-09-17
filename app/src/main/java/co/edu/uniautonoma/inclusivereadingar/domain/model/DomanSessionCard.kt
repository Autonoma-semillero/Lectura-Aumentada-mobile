package co.edu.uniautonoma.inclusivereadingar.domain.model

data class DomanSessionCard(
    val id: String,
    val orderIndex: Int,
    val word: String,
    val status: String,
    val audioUrl: String?,
    val categoryId: String?,
    val learningUnitId: String?
)
