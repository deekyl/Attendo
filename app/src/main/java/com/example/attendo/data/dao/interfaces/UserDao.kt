package com.example.attendo.data.dao.interfaces

import com.example.attendo.data.model.user.User

interface UserDao {
    suspend fun getUserById(userId: String): User?
    suspend fun isUserActive(userId: String): Boolean
    suspend fun updateUser(user: User): Boolean
    suspend fun getAllUsers(): List<User>
    suspend fun getAllUsersIncludingInactive(): List<User>
    suspend fun createUser(user: User): User?
    suspend fun toggleUserStatus(userId: String, isActive: Boolean): Boolean
    suspend fun deleteUser(userId: String): Boolean
}