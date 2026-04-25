package co.edu.uniautonoma.inclusivereadingar.domain.model

data class AuthSession(
    val accessToken: String,
    val user: SessionUser
)
