package com.example.attendo.data.model.user

sealed class UserState {
    data object Loading : UserState()
    data class Regular(val user: User) : UserState()
    data class Admin(val user: User) : UserState()
    data object Inactive : UserState()
    data class Error(val message: String) : UserState()
}