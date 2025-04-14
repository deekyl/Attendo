package com.example.attendo.data.repositories


import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl(
    private val client: SupabaseClient
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
                Result.failure(Exception("Cannot obtain user information"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String): Result<UserInfo> {
        return try {
          client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val user = client.auth.currentUserOrNull()
            if(user != null){
                Result.success(user)
            } else{
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


}
