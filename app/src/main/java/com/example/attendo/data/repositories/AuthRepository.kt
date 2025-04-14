package com.example.attendo.data.repositories

import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<UserInfo>
    suspend fun signUp(email: String, password: String): Result<UserInfo>
    suspend fun logout(): Result<Unit>
    fun getCurrentUser(): UserInfo?

}


