package co.edu.uniautonoma.inclusivereadingar.domain.model

data class LearningUnit(
    val id: String,
    val word: String,
    val category: String,
    val markerId: String,
    val model3dPath: String,
    val audioPath: String,
    val altText: String
)
