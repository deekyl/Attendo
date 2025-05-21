package com.example.attendo.data.dao.interfaces

interface ProfileImageDao {
    suspend fun uploadProfileImage(userId: String, imageBytes: ByteArray): String?
    suspend fun getProfileImageUrl(userId: String): String?
    suspend fun deleteProfileImage(userId: String): Boolean
}