package com.example.attendo.data.repositories

interface AuthRepository {
    suspend fun login(email: String, password: String): Boolean
    suspend fun signUp(email: String, password: String): Boolean
}