package com.example.attendo.data.model.user

data class User(
    val userId: String,
    val fullName: String,
    val profileImage: String,
    val email: String,
    val documentId: String,
    val isAdmin: Boolean = false,
    val isActive: Boolean = true
)