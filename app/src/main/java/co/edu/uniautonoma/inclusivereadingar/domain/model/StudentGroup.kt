package co.edu.uniautonoma.inclusivereadingar.domain.model

data class StudentGroup(
    val id: String,
    val name: String,
    val normalizedName: String,
    val description: String?,
    val teacherId: String,
    val status: String,
    val createdBy: String,
    val studentIds: List<String>,
    val createdAt: String?,
    val updatedAt: String?
)

sealed interface AudienceSearchItem {
    val audienceKey: String
    val id: String
}

data class AudienceStudent(
    override val audienceKey: String,
    override val id: String,
    val displayName: String?,
    val email: String,
    val username: String?,
    val groupIds: List<String>,
    val unassigned: Boolean
) : AudienceSearchItem {
    val label: String
        get() = displayName?.takeIf { it.isNotBlank() }
            ?: username?.takeIf { it.isNotBlank() }
            ?: email
}

data class AudienceGroup(
    override val audienceKey: String,
    override val id: String,
    val name: String,
    val description: String?,
    val teacherId: String,
    val status: String,
    val studentIds: List<String>
) : AudienceSearchItem

data class AudienceSearchResult(
    val items: List<AudienceSearchItem>
)
