package com.example.attendo.data.repositories.repository

import com.example.attendo.data.model.user.User
import io.github.jan.supabase.auth.user.UserInfo

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<UserInfo>
    suspend fun signUp(email: String, password: String): Result<UserInfo>
    suspend fun logout(): Result<Unit>
    suspend fun getUserDetails(userId: String): Result<User>
    suspend fun checkUserStatus(userId: String): Result<User>
    fun getCurrentUser(): UserInfo?
}