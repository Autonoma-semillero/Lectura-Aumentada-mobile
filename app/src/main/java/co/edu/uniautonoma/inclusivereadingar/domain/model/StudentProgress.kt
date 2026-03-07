package co.edu.uniautonoma.inclusivereadingar.domain.model

import java.time.Instant

data class StudentProgress(
    val studentId: String,
    val learningUnitId: String,
    val markerId: String,
    val success: Boolean,
    val timestamp: Instant
)
