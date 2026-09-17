package co.edu.uniautonoma.inclusivereadingar.domain.model

data class Category(
    val id: String,
    val name: String,
    val slug: String,
    val description: String?,
    val icon: String?,
    val sortOrder: Int,
    val wordCardsCount: Int
)
