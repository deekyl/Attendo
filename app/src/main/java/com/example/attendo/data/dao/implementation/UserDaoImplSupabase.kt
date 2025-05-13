package com.example.attendo.data.dao.implementation

import android.util.Log
import com.example.attendo.data.dao.interfaces.UserDao
import com.example.attendo.data.model.user.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class UserDaoImplSupabase(
    private val client: SupabaseClient
) : UserDao {
    override suspend fun getUserById(userId: String): User? {
        return try {
            withContext(Dispatchers.IO) {
                client.postgrest
                    .from("users")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeSingle<User>()
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                // Propagar explícitamente para manejo adecuado
                throw e
            }
            Log.e("attendo", "Error getting user: ${e.message}", e)
            null
        }
    }


    override suspend fun isUserActive(userId: String): Boolean {
        return try {
            val user = getUserById(userId)
            user?.isActive ?: false
        } catch (e: Exception) {
            Log.e("attendo", "Error checking if user is active: ${e.message}", e)
            false
        }
    }

    override suspend fun updateUser(user: User): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getAllUsers(): List<User> {
        return try {
            // Usamos withContext para asegurarnos de manejar la cancelación correctamente
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.postgrest
                    .from("users")
                    .select {
                        filter {
                            eq("is_active", true)
                        }
                        order("full_name", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                    }
                    .decodeList<User>()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                // Propagamos la excepción de cancelación para manejarla adecuadamente
                throw e
            }
            Log.e("attendo", "Error getting all users: ${e.message}", e)
            emptyList()
        }
    }
}