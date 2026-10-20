package co.edu.uniautonoma.inclusivereadingar.domain.model

/** A reusable card. It is content, never a record of a child's performance. */
data class LiveDomanCard(
    val id: String,
    val text: String,
    val categoryId: String?,
    val status: String
)

data class LiveDomanRepertoire(
    val id: String,
    val name: String,
    val description: String?,
    val cardCount: Int,
    val status: String,
    val categoryId: String? = null
)

/** The server is the source of truth for the card shown to every participant. */
data class LiveDomanSession(
    val id: String,
    val groupId: String,
    val repertoireId: String,
    val repertoireName: String?,
    val status: String,
    val sequence: Int,
    val displayText: String?,
    val displayAudioUrl: String?,
    val displayDurationMs: Int
)

data class LiveDomanParticipant(
    val studentId: String,
    val status: String,
    val acknowledgedSequence: Int
)

data class LiveDomanSessionDetail(
    val session: LiveDomanSession,
    val participants: List<LiveDomanParticipant>
)
