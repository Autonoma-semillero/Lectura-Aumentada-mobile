package co.edu.uniautonoma.inclusivereadingar.data.remote

import co.edu.uniautonoma.inclusivereadingar.MainDispatcherRule
import co.edu.uniautonoma.inclusivereadingar.data.repository.AuthSessionRepository
import co.edu.uniautonoma.inclusivereadingar.domain.model.AuthSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.SessionUser
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.AuthViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthApiTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun login_updatesStateAndInvokesSuccessCallback() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)
        var loginSuccess = false

        viewModel.updateEmail("student@lectura.app")
        viewModel.updatePassword("Lectura123!")
        viewModel.login { loginSuccess = true }
        advanceUntilIdle()

        assertThat(loginSuccess).isTrue()
        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isNull()
        assertThat(repository.savedEmail).isEqualTo("student@lectura.app")
    }
}

private class FakeAuthRepository : AuthSessionRepository {
    private val _sessionFlow = MutableStateFlow<AuthSession?>(null)
    var savedEmail: String? = null

    override val sessionFlow: Flow<AuthSession?> = _sessionFlow

    override suspend fun login(email: String, password: String): AuthSession {
        savedEmail = email
        val session = AuthSession(
            accessToken = "token-123",
            user = SessionUser(
                id = "user-1",
                email = email,
                displayName = "Demo Student",
                roles = listOf("student"),
                status = "active"
            )
        )
        _sessionFlow.value = session
        return session
    }

    override suspend fun logout() {
        _sessionFlow.value = null
    }

    override suspend fun currentSession(): AuthSession? = _sessionFlow.value
}
