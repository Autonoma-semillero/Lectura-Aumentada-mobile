package co.edu.uniautonoma.inclusivereadingar.domain.model

data class AudioUploadInput(
    val filePath: String,
    val mimeType: String?,
    val originalName: String?
)

