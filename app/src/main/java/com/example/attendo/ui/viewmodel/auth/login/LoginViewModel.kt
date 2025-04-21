package com.example.attendo.ui.viewmodel.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendo.data.model.auth.AuthUiState
import com.example.attendo.data.model.auth.ValidationResult
import com.example.attendo.data.repositories.repository.AuthRepository
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()

    private val _isAdmin = MutableStateFlow<Boolean>(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    init {
        _currentUser.value = authRepository.getCurrentUser()
        // BORRAR
        _currentUser.value = null
        if (_currentUser.value != null) {
            _uiState.value = AuthUiState.Success
        }
    }

    fun login(email: String, password: String) {
        val validationResult = validateForm(email, password)

        if (validationResult is ValidationResult.Error) {
            _uiState.value = AuthUiState.Error(validationResult.message)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            authRepository.login(email, password).fold(
                onSuccess = { userInfo ->
                    _currentUser.value = userInfo

                    // Verificar si el usuario está activo
                    authRepository.checkUserStatus(userInfo.id).fold(
                        onSuccess = { user ->
                            // Usuario activo, verificar si es admin
                            _isAdmin.value = user.isAdmin
                            _uiState.value = AuthUiState.Success
                        },
                        onFailure = { error ->
                            _uiState.value = AuthUiState.Error(error.message ?: "Error verificando estado de usuario")
                            authRepository.logout()
                            _currentUser.value = null
                        }
                    )
                },
                onFailure = { error ->
                    val errorMessage = error.message ?: "Error desconocido al iniciar sesión"
                    _uiState.value = AuthUiState.Error(errorMessage)
                }
            )
        }
    }

    // VALIDATIONS FUNCTIONS

    // Check email
    fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex()) && email.isNotEmpty()
    }

    // Check min pass length
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    // Check form null
    fun validateForm(email: String, password: String): ValidationResult {
        return when {
            email.isEmpty() -> ValidationResult.Error("El email no puede estar vacío")
            !isValidEmail(email) -> ValidationResult.Error("El formato del email no es válido")
            password.isEmpty() -> ValidationResult.Error("La contraseña no puede estar vacía")
            !isValidPassword(password) -> ValidationResult.Error("La contraseña debe tener al menos 6 caracteres")
            else -> ValidationResult.Success
        }
    }
}