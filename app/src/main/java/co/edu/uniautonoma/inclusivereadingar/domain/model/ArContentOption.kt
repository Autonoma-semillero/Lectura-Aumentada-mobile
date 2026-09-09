package co.edu.uniautonoma.inclusivereadingar.domain.model

data class ArModelOption(
    val learningUnitId: String,
    val markerId: String,
    val word: String,
    val model3dUrl: String
)

data class ArMarkerOption(
    val markerId: String,
    val label: String
)

val SUPPORTED_AR_MARKERS = listOf(
    ArMarkerOption(markerId = "demo-animales-gato", label = "Hiro"),
    ArMarkerOption(markerId = "demo-animales-perro", label = "Kanji")
)
