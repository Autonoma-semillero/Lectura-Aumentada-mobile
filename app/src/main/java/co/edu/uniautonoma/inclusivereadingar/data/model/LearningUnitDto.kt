package co.edu.uniautonoma.inclusivereadingar.data.model

import co.edu.uniautonoma.inclusivereadingar.domain.model.LearningUnit

data class LearningUnitDto(
    val id: String,
    val word: String,
    val category: String,
    val markerId: String,
    val model3dPath: String,
    val audioPath: String,
    val altText: String
)

fun LearningUnitDto.toDomain(): LearningUnit {
    return LearningUnit(id, word, category, markerId, model3dPath, audioPath, altText)
}
