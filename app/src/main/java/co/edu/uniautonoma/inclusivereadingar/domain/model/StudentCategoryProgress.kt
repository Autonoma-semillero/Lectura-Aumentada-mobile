package co.edu.uniautonoma.inclusivereadingar.domain.model

data class StudentCategoryProgress(
    val categoryId: String,
    val categoryName: String,
    val categorySlug: String,
    val total: Int,
    val byStatus: CategoryStatusCounts,
    val phase2Ready: Boolean
) {
    val activeWords: Int get() = total - byStatus.archived
    val completedFraction: Float get() = if (activeWords > 0) byStatus.completed.toFloat() / activeWords else 0f
    val activeFraction: Float get() = if (activeWords > 0) byStatus.active.toFloat() / activeWords else 0f
}

data class CategoryStatusCounts(
    val new: Int,
    val active: Int,
    val completed: Int,
    val archived: Int
)
