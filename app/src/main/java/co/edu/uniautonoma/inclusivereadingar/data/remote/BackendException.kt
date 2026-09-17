package co.edu.uniautonoma.inclusivereadingar.data.remote

class BackendException(
    val statusCode: Int,
    override val message: String
) : Exception(message)
