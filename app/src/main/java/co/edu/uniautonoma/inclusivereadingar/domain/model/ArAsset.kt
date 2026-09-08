package co.edu.uniautonoma.inclusivereadingar.domain.model

data class ArAsset(
    val id: String,
    val learningUnitId: String,
    val markerId: String,
    val word: String,
    val model3dUrl: String?,
    val audioUrl: String?,
    val accessibilityLabel: String?
)
