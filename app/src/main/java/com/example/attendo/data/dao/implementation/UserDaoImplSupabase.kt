package com.example.attendo.data.dao.implementation

import android.util.Log
import com.example.attendo.data.dao.interfaces.UserDao
import com.example.attendo.data.model.user.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class UserDaoImplSupabase(
    private val client: SupabaseClient
) : UserDao {
    override suspend fun getUserById(userId: String): User? {
        return try {
            client.postgrest
                .from("users")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingle<User>()
        } catch (e: Exception) {
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
}