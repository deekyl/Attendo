// Data layer: AuthRepositoryImpl.kt
package com.example.attendo.data.repositories

import com.example.attendo.data.repositories.AuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email


class AuthRepositoryImpl(
    private val auth: Auth
) : AuthRepository {
    override suspend fun login(email: String, password: String): Boolean {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (e: Exception) {
            false
        }

    }

    override suspend fun signUp(email: String, password: String): Boolean {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
