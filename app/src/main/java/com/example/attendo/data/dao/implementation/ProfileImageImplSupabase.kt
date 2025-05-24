package com.example.attendo.data.dao.implementation

import android.util.Log
import com.example.attendo.data.dao.interfaces.ProfileImageDao
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.delay

class ProfileImageDaoImplSupabase(
    private val client: SupabaseClient
) : ProfileImageDao {

    private val bucketName = "profile_images"

    override suspend fun uploadProfileImage(userId: String, imageBytes: ByteArray): String? {
        return try {
            // Eliminar imágenes anteriores
            deleteProfileImage(userId)

            // Esperar un poco para asegurar que la eliminación se complete
            delay(500)

            // Nombre único para el archivo
            val fileName = "$userId-${System.currentTimeMillis()}.jpg"

            // Subir la imagen
            val bucket = client.storage.from(bucketName)
            bucket.upload(fileName, imageBytes) {
                upsert = true
            }

            // Obtener la URL pública
            val publicUrl = bucket.publicUrl(fileName)
            Log.d("Attendo", "Imagen subida correctamente: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e("Attendo", "Error subiendo imagen de perfil: ${e.message}", e)
            null
        }
    }

    override suspend fun getProfileImageUrl(userId: String): String? {
        return try {
            // Listar archivos en el bucket
            val bucket = client.storage.from(bucketName)
            val files = bucket.list("")

            // Buscar archivos que empiecen con el userId
            val userFile = files.find { it.name.startsWith(userId) }

            if (userFile != null) {
                val publicUrl = bucket.publicUrl(userFile.name)
                Log.d("Attendo", "URL de imagen obtenida: $publicUrl")
                publicUrl
            } else {
                Log.d("Attendo", "No se encontraron imágenes para el usuario $userId")
                null
            }
        } catch (e: Exception) {
            Log.e("Attendo", "Error obteniendo URL de imagen: ${e.message}", e)
            null
        }
    }

    override suspend fun deleteProfileImage(userId: String): Boolean {
        return try {
            // Listar archivos en el bucket
            val bucket = client.storage.from(bucketName)
            val files = bucket.list("")

            // Buscar archivos que empiecen con el userId
            val userFiles = files.filter { it.name.startsWith(userId) }

            if (userFiles.isNotEmpty()) {
                bucket.delete(userFiles.map { it.name })
                Log.d("Attendo", "Imágenes eliminadas para el usuario $userId")
                true
            } else {
                Log.d("Attendo", "No se encontraron imágenes para eliminar")
                false
            }
        } catch (e: Exception) {
            Log.e("Attendo", "Error eliminando imágenes: ${e.message}", e)
            false
        }
    }
}