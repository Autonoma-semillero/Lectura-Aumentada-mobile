package co.edu.uniautonoma.inclusivereadingar.domain.model

data class SessionUser(
    val id: String,
    val email: String,
    val displayName: String?,
    val roles: List<String>,
    val status: String?
)
