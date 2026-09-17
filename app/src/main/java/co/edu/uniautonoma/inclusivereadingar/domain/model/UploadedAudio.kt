package co.edu.uniautonoma.inclusivereadingar.domain.model

data class UploadedAudio(
    val audioUrl: String,
    val mimeType: String,
    val sizeBytes: Long,
    val originalName: String
)

