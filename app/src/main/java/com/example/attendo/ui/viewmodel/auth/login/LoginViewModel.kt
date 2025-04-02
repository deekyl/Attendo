package com.example.attendo.ui.viewmodel.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class LoginViewModel : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState?>(null)
    val loginState: StateFlow<LoginState?> get() = _loginState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            _loginState.value = if (email.isNotEmpty() && password.isNotEmpty()) {
                LoginState.Success
            } else {
                LoginState.Error("Credenciales inválidas")
            }
        }
    }

    fun clearLoginState() {
        _loginState.value = null
    }
}

