package com.example.attendo.ui.viewmodel.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.attendo.data.model.AuthUiState
import com.example.attendo.data.repositories.AuthRepository
import com.example.attendo.data.repositories.AuthRepositoryImpl
import io.github.jan.supabase.SupabaseClient
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

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            authRepository.login(email, password).fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _uiState.value = AuthUiState.Success
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            authRepository.signUp(email, password).fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _uiState.value = AuthUiState.Success
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    class LoginViewModelFactory(
        private val client: SupabaseClient
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                val repository = AuthRepositoryImpl(client)
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
