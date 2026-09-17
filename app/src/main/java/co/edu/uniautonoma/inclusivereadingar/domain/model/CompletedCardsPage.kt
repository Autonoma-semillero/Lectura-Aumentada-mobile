package co.edu.uniautonoma.inclusivereadingar.domain.model

import kotlin.math.ceil

data class CompletedCardsPage(
    val data: List<CompletedCard>,
    val total: Int,
    val page: Int,
    val limit: Int
) {
    val totalPages: Int get() = if (limit > 0) ceil(total.toDouble() / limit).toInt() else 0
    val hasMore: Boolean get() = page < totalPages
}
