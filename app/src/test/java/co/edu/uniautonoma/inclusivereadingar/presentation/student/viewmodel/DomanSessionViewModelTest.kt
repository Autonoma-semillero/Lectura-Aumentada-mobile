package co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel

import co.edu.uniautonoma.inclusivereadingar.MainDispatcherRule
import co.edu.uniautonoma.inclusivereadingar.data.remote.BackendException
import co.edu.uniautonoma.inclusivereadingar.data.repository.DomanSessionDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.DomanSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.DomanSessionCard
import co.edu.uniautonoma.inclusivereadingar.domain.model.OngoingDomanSession
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
class DomanSessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun resumeOrStart_missingStoredSession_clearsSnapshotAndPreparesAnotherSession() = runTest {
        val repository = FakeDomanSessionDataSource(
            ongoingSession = OngoingDomanSession(
                sessionId = "deleted-session",
                categoryId = "nature",
                categoryName = "Naturaleza",
                currentCardIndex = 3
            ),
            preparedSession = session(sessionId = "replacement-session"),
            loadFailure = BackendException(404, "La sesión ya no existe.")
        )
        val viewModel = DomanSessionViewModel(repository)

        viewModel.resumeOrStart(categoryId = "nature", categoryName = "Naturaleza")
        advanceUntilIdle()

        assertThat(repository.loadedSessionIds).containsExactly("deleted-session")
        assertThat(repository.clearOngoingSessionCalls).isEqualTo(1)
        assertThat(repository.preparedCategoryIds).containsExactly("nature")
        assertThat(repository.savedSnapshot?.sessionId).isEqualTo("replacement-session")
        assertThat(viewModel.uiState.value.session?.sessionId).isEqualTo("replacement-session")
        assertThat(viewModel.uiState.value.currentIndex).isEqualTo(0)
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    @Test
    fun resumeOrStart_backendFailure_keepsItsMessageInTheUiState() = runTest {
        val repository = FakeDomanSessionDataSource(
            prepareFailure = BackendException(409, "No hay sesiones pendientes para Naturaleza.")
        )
        val viewModel = DomanSessionViewModel(repository)

        viewModel.resumeOrStart(categoryId = "nature", categoryName = "Naturaleza")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.session).isNull()
        assertThat(state.errorMessage).isEqualTo("No hay sesiones pendientes para Naturaleza.")
    }

    @Test
    fun resumeOrStart_nonMissingStoredSessionFailure_doesNotReplaceIt() = runTest {
        val repository = FakeDomanSessionDataSource(
            ongoingSession = OngoingDomanSession(
                sessionId = "unavailable-session",
                categoryId = "nature",
                categoryName = "Naturaleza",
                currentCardIndex = 1
            ),
            loadFailure = BackendException(503, "El servicio no está disponible.")
        )
        val viewModel = DomanSessionViewModel(repository)

        viewModel.resumeOrStart(categoryId = "nature", categoryName = "Naturaleza")
        advanceUntilIdle()

        assertThat(repository.clearOngoingSessionCalls).isEqualTo(0)
        assertThat(repository.preparedCategoryIds).isEmpty()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("El servicio no está disponible.")
    }

    @Test
    fun retry_advanceFailure_keepsLoadedSessionAndReenablesItsTimer() = runTest {
        val repository = FakeDomanSessionDataSource(
            preparedSession = session("current-session"),
            cardCompletedFailure = BackendException(503, "No se pudo registrar la tarjeta.")
        )
        val viewModel = DomanSessionViewModel(repository)
        viewModel.resumeOrStart(categoryId = "nature", categoryName = "Naturaleza")
        advanceUntilIdle()

        viewModel.advance()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.session?.sessionId).isEqualTo("current-session")
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("No se pudo registrar la tarjeta.")
        assertThat(viewModel.uiState.value.canRunTimer).isFalse()

        viewModel.retry(categoryId = "nature", categoryName = "Naturaleza")

        assertThat(viewModel.uiState.value.session?.sessionId).isEqualTo("current-session")
        assertThat(viewModel.uiState.value.errorMessage).isNull()
        assertThat(viewModel.uiState.value.canRunTimer).isTrue()
        assertThat(repository.preparedCategoryIds).containsExactly("nature")
    }

    @Test
    fun advance_missingBackendSession_discardsItAndRetryPreparesAnotherOne() = runTest {
        val repository = FakeDomanSessionDataSource(
            preparedSession = session("deleted-session"),
            cardCompletedFailure = BackendException(404, "La sesión ya no existe.")
        )
        val viewModel = DomanSessionViewModel(repository)
        viewModel.resumeOrStart(categoryId = "nature", categoryName = "Naturaleza")
        advanceUntilIdle()

        viewModel.advance()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.session).isNull()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("La sesión ya no existe.")
        assertThat(repository.currentOngoingSession).isNull()

        repository.cardCompletedFailure = null
        repository.preparedSession = session("replacement-session")
        viewModel.retry(categoryId = "nature", categoryName = "Naturaleza")
        advanceUntilIdle()

        assertThat(repository.preparedCategoryIds).containsExactly("nature", "nature").inOrder()
        assertThat(viewModel.uiState.value.session?.sessionId).isEqualTo("replacement-session")
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    @Test
    fun skip_missingSessionWhileCompleting_discardsTheLoadedSession() = runTest {
        val repository = FakeDomanSessionDataSource(
            preparedSession = session("deleted-before-complete"),
            completeFailure = BackendException(404, "La sesión ya no existe.")
        )
        val viewModel = DomanSessionViewModel(repository)
        viewModel.resumeOrStart(categoryId = "nature", categoryName = "Naturaleza")
        advanceUntilIdle()

        viewModel.skip()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.session).isNull()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("La sesión ya no existe.")
        assertThat(repository.currentOngoingSession).isNull()
    }

    @Test
    fun resumeOrStart_sameCategoryWhileLoading_doesNotPrepareTwice() = runTest {
        val repository = OutOfOrderDomanSessionDataSource()
        val viewModel = DomanSessionViewModel(repository)

        viewModel.resumeOrStart(categoryId = "nature", categoryName = "Naturaleza")
        runCurrent()
        viewModel.resumeOrStart(categoryId = "nature", categoryName = "Naturaleza")
        runCurrent()

        assertThat(repository.preparedCategoryIds).containsExactly("nature")

        repository.completePreparation(
            categoryId = "nature",
            session = session(sessionId = "nature-session")
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.session?.sessionId).isEqualTo("nature-session")
    }

    @Test
    fun resumeOrStart_categoryChanges_ignoresTheOlderResponse() = runTest {
        val repository = OutOfOrderDomanSessionDataSource()
        val viewModel = DomanSessionViewModel(repository)

        viewModel.resumeOrStart(categoryId = "nature", categoryName = "Naturaleza")
        runCurrent()
        viewModel.resumeOrStart(categoryId = "animals", categoryName = "Animales")
        runCurrent()

        repository.completePreparation(
            categoryId = "animals",
            session = session(sessionId = "animals-session", categoryId = "animals")
        )
        runCurrent()
        assertThat(viewModel.uiState.value.session?.sessionId).isEqualTo("animals-session")

        repository.completePreparation(
            categoryId = "nature",
            session = session(sessionId = "nature-session", categoryId = "nature")
        )
        advanceUntilIdle()

        assertThat(repository.preparedCategoryIds).containsExactly("nature", "animals").inOrder()
        assertThat(viewModel.uiState.value.session?.sessionId).isEqualTo("animals-session")
        assertThat(repository.savedSnapshot?.categoryId).isEqualTo("animals")
    }

    @Test
    fun resumeOrStart_emptySession_clearsItAndAllowsPreparingAgain() = runTest {
        val repository = FakeDomanSessionDataSource(
            preparedSession = session("empty-session").copy(cards = emptyList())
        )
        val viewModel = DomanSessionViewModel(repository)

        viewModel.resumeOrStart(categoryId = "nature", categoryName = "Naturaleza")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.session).isNull()
        assertThat(viewModel.uiState.value.errorMessage)
            .isEqualTo("La sesión no contiene tarjetas disponibles. Intenta de nuevo.")
        assertThat(viewModel.uiState.value.canRunTimer).isFalse()
        assertThat(repository.clearOngoingSessionCalls).isEqualTo(1)
        assertThat(repository.savedSnapshot).isNull()

        repository.preparedSession = session("usable-session")
        viewModel.retry(categoryId = "nature", categoryName = "Naturaleza")
        advanceUntilIdle()

        assertThat(repository.preparedCategoryIds).containsExactly("nature", "nature").inOrder()
        assertThat(viewModel.uiState.value.session?.sessionId).isEqualTo("usable-session")
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }
}

private class FakeDomanSessionDataSource(
    private var ongoingSession: OngoingDomanSession? = null,
    var preparedSession: DomanSession = session("prepared-session"),
    private val loadFailure: Throwable? = null,
    private val prepareFailure: Throwable? = null,
    var cardCompletedFailure: Throwable? = null,
    private val completeFailure: Throwable? = null
) : DomanSessionDataSource {
    val loadedSessionIds = mutableListOf<String>()
    val preparedCategoryIds = mutableListOf<String>()
    var clearOngoingSessionCalls = 0
        private set
    var savedSnapshot: OngoingDomanSession? = null
        private set
    val currentOngoingSession: OngoingDomanSession?
        get() = ongoingSession

    override suspend fun prepareSession(categoryId: String): DomanSession {
        preparedCategoryIds += categoryId
        prepareFailure?.let { throw it }
        return preparedSession
    }

    override suspend fun loadSession(sessionId: String): DomanSession {
        loadedSessionIds += sessionId
        loadFailure?.let { throw it }
        return preparedSession
    }

    override suspend fun registerCardShown(sessionId: String, wordCardId: String, displayMs: Int) = Unit

    override suspend fun registerCardCompleted(sessionId: String, wordCardId: String) {
        cardCompletedFailure?.let { throw it }
    }

    override suspend fun registerCardSkipped(sessionId: String, wordCardId: String) = Unit

    override suspend fun registerAudioPlayed(sessionId: String, wordCardId: String) = Unit

    override suspend fun completeSession(sessionId: String): DomanSession {
        completeFailure?.let { throw it }
        return preparedSession
    }

    override suspend fun saveOngoingSession(snapshot: OngoingDomanSession) {
        savedSnapshot = snapshot
        ongoingSession = snapshot
    }

    override suspend fun getOngoingSession(): OngoingDomanSession? = ongoingSession

    override suspend fun clearOngoingSession() {
        clearOngoingSessionCalls++
        ongoingSession = null
    }
}

private class OutOfOrderDomanSessionDataSource : DomanSessionDataSource {
    val preparedCategoryIds = mutableListOf<String>()
    var savedSnapshot: OngoingDomanSession? = null
        private set
    private val preparationContinuations = mutableMapOf<String, Continuation<DomanSession>>()

    override suspend fun prepareSession(categoryId: String): DomanSession = suspendCoroutine { continuation ->
        preparedCategoryIds += categoryId
        preparationContinuations[categoryId] = continuation
    }

    fun completePreparation(categoryId: String, session: DomanSession) {
        requireNotNull(preparationContinuations.remove(categoryId)).resume(session)
    }

    override suspend fun loadSession(sessionId: String): DomanSession = error("No snapshot was configured")

    override suspend fun registerCardShown(sessionId: String, wordCardId: String, displayMs: Int) = Unit

    override suspend fun registerCardCompleted(sessionId: String, wordCardId: String) = Unit

    override suspend fun registerCardSkipped(sessionId: String, wordCardId: String) = Unit

    override suspend fun registerAudioPlayed(sessionId: String, wordCardId: String) = Unit

    override suspend fun completeSession(sessionId: String): DomanSession = error("Not used")

    override suspend fun saveOngoingSession(snapshot: OngoingDomanSession) {
        savedSnapshot = snapshot
    }

    override suspend fun getOngoingSession(): OngoingDomanSession? = null

    override suspend fun clearOngoingSession() = Unit
}

private fun session(sessionId: String, categoryId: String = "nature"): DomanSession = DomanSession(
    sessionId = sessionId,
    studentId = "student-1",
    planId = "plan-1",
    categoryId = categoryId,
    sessionIndex = 1,
    status = "in_progress",
    displayMs = 2_000,
    audioMode = "manual",
    mode = "auto",
    cards = listOf(
        DomanSessionCard(
            id = "card-1",
            orderIndex = 0,
            word = "ÁRBOL",
            status = "active",
            audioUrl = null,
            categoryId = categoryId,
            learningUnitId = "unit-1"
        )
    )
)
