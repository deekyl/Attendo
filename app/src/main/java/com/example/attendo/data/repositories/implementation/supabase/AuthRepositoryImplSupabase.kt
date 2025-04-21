package com.example.attendo.data.repositories.implementation.supabase

import android.util.Log
import com.example.attendo.data.dao.interfaces.UserDao
import com.example.attendo.data.model.user.User
import com.example.attendo.data.repositories.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo

class AuthRepositoryImplSupabase(
    private val client: SupabaseClient,
    private val userDao: UserDao
) : AuthRepository {

    override suspend fun login(mail: String, pass: String): Result<UserInfo> {
        return try {
            client.auth.signInWith(Email) {
                this.email = mail
                this.password = pass
            }
            val user = client.auth.currentUserOrNull()
            if (user != null) {
                Log.e("attendo", "Login -> OK")
                Result.success(user)
            } else {
                Result.failure(Exception("No se pudo iniciar sesión. Verifica tus credenciales."))
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("invalid_credentials") == true ->
                    "Credenciales inválidas. Por favor, verifica tu email y contraseña."

                e.message?.contains("network") == true ||
                        e.message?.contains("connection") == true ->
                    "Error de conexión. Verifica tu conexión a internet."

                else -> "Error al iniciar sesión: ${e.javaClass.simpleName}"
            }

            Log.e("attendo", "Login error: ${e.message}", e)
            Result.failure(Exception(errorMessage))
        }
    }

    override suspend fun signUp(email: String, password: String): Result<UserInfo> {
        return try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val user = client.auth.currentUserOrNull()
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Registration successful but email verification required"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUser(): UserInfo? {
        return try {
            client.auth.currentUserOrNull()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getUserDetails(userId: String): Result<User> {
        return try {
            val user = userDao.getUserById(userId)
                ?: return Result.failure(Exception("Usuario no encontrado"))

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkUserStatus(userId: String): Result<User> {
        return try {
            val user = userDao.getUserById(userId)
                ?: return Result.failure(Exception("Usuario no encontrado"))

            if (!user.isActive) {
                return Result.failure(Exception("Tu cuenta está bloqueada. Contacta con el administrador."))
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}