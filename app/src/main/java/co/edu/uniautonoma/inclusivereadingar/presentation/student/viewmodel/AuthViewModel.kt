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
    val identifier: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthSessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateIdentifier(value: String) {
        _uiState.update { it.copy(identifier = value, errorMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun login(onSuccess: () -> Unit) {
        val snapshot = _uiState.value
        if (snapshot.identifier.isBlank() || snapshot.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa tu usuario o correo y tu contraseña.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                authRepository.login(snapshot.identifier, snapshot.password)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, password = "") }
                onSuccess()
            }.onFailure { error ->
                val message = when (error) {
                    is BackendException -> error.message
                    else -> error.message?.takeIf { it.isNotBlank() }
                        ?: "No fue posible iniciar sesion."
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
