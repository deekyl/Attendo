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
        return try {
            withContext(Dispatchers.IO) {
                client.postgrest
                    .from("users")
                    .update({
                        set("full_name", user.fullName)
                        set("email", user.email)
                        set("document_id", user.documentId)
                        set("address", user.address)
                    }) {
                        filter {
                            eq("user_id", user.userId)
                        }
                    }
                Log.d("Attendo", "Usuario actualizado correctamente: ${user.userId}")
                true
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            Log.e("Attendo", "Error actualizando usuario: ${e.message}", e)
            false
        }
    }

    override suspend fun getAllUsers(): List<User> {
        return try {
            withContext(Dispatchers.IO) {
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
                throw e
            }
            Log.e("Attendo", "Error getting all users: ${e.message}", e)
            emptyList()
        }
    }
}