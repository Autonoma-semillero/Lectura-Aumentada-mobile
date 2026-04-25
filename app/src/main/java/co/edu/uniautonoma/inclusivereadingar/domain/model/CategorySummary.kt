package co.edu.uniautonoma.inclusivereadingar.domain.model

data class CategorySummary(
    val id: String,
    val name: String,
    val slug: String,
    val description: String?,
    val availableWordCardsCount: Int
)
