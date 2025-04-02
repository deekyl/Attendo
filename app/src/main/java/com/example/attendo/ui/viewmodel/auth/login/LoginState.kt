package com.example.attendo.ui.viewmodel.auth.login

sealed class LoginState {
    data object Success : LoginState()
    data class Error(val message: String) : LoginState()
    data object Loading : LoginState()
}