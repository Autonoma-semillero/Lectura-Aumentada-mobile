package co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.remote.BackendException
import co.edu.uniautonoma.inclusivereadingar.data.repository.AuthSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val pin: String = "",
    val identifier: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pinError: String? = null,
    val identifierError: String? = null,
    val passwordError: String? = null
)

class AuthViewModel(
    private val authRepository: AuthSessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updatePin(value: String) {
        _uiState.update { it.copy(pin = value.filter(Char::isDigit).take(6), errorMessage = null, pinError = null) }
    }

    fun updateIdentifier(value: String) {
        _uiState.update { it.copy(identifier = value, errorMessage = null, identifierError = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null, passwordError = null) }
    }

    fun loginWithStudentPin(onSuccess: () -> Unit) {
        val snapshot = _uiState.value
        if (snapshot.pin.length !in 4..6) {
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    pinError = "Escribe los 4 a 6 números de tu PIN."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    pinError = null
                )
            }
            runCatching {
                authRepository.loginWithStudentPin(snapshot.pin)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, pin = "") }
                onSuccess()
            }.onFailure { error ->
                val message = when ((error as? BackendException)?.statusCode) {
                    401, 403 -> "Ese PIN no abre la puerta todavía. Pide ayuda a tu docente."
                    408, 429, 500, 502, 503, 504 -> "No pudimos entrar ahora. Revisa internet e inténtalo de nuevo."
                    else -> "No pudimos entrar. Inténtalo de nuevo o pide ayuda a tu docente."
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                }
            }
        }
    }

    fun loginWithPassword(onSuccess: () -> Unit) {
        val snapshot = _uiState.value
        if (snapshot.identifier.isBlank() || snapshot.password.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    identifierError = if (snapshot.identifier.isBlank()) "Escribe tu usuario o correo." else null,
                    passwordError = if (snapshot.password.isBlank()) "Escribe tu contraseña." else null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    identifierError = null,
                    passwordError = null
                )
            }
            runCatching {
                authRepository.login(snapshot.identifier, snapshot.password)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, password = "") }
                onSuccess()
            }.onFailure { error ->
                val message = when ((error as? BackendException)?.statusCode) {
                    401, 403 -> "Revisa tu usuario y contraseña."
                    408, 429, 500, 502, 503, 504 -> "No pudimos entrar ahora. Revisa internet e inténtalo de nuevo."
                    else -> "No pudimos entrar. Inténtalo de nuevo."
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            }
        }
    }
}

class AuthViewModelFactory(
    private val authRepository: AuthSessionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
