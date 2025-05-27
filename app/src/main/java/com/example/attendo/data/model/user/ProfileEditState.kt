package com.example.attendo.data.model.user

sealed class ProfileEditState {
    data object Idle : ProfileEditState()
    data object Loading : ProfileEditState()
    data object Success : ProfileEditState()
    data class Error(val message: String) : ProfileEditState()
}

data class ProfileEditData(
    val fullName: String = "",
    val email: String = "",
    val documentId: String = "",
    val address: String = ""
)