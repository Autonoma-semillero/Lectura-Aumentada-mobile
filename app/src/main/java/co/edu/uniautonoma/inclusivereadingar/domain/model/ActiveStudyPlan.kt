package co.edu.uniautonoma.inclusivereadingar.domain.model

data class ActiveStudyPlan(
    val planId: String,
    val planName: String?,
    val levelId: String?,
    val levelName: String?,
    val date: String?,
    val categories: List<ActiveStudyPlanCategory>
)

data class ActiveStudyPlanCategory(
    val id: String,
    val name: String,
    val slug: String?,
    val description: String?,
    val icon: String?,
    val availableWordCardsCount: Int
)
