package co.edu.uniautonoma.inclusivereadingar.domain.model

data class AppUser(
    val id: String,
    val email: String,
    val displayName: String?,
    val roles: List<String>
)
