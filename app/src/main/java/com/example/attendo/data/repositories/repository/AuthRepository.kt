package com.example.attendo.data.repositories.repository

import com.example.attendo.data.model.user.User
import io.github.jan.supabase.auth.user.UserInfo

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<UserInfo>
    suspend fun signUp(email: String, password: String): Result<UserInfo>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUserDetails(): Result<User>
    suspend fun checkUserStatus(userId: String): Result<User>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
    fun getCurrentUser(): UserInfo?
}