package com.example.attendo.data.dao.implementation

import android.util.Log
import com.example.attendo.data.dao.interfaces.ProfileImageDao
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import java.util.UUID

class ProfileImageDaoImplSupabase(
    private val client: SupabaseClient
) : ProfileImageDao {

    private val bucketName = "profile_images"

    override suspend fun uploadProfileImage(userId: String, imageBytes: ByteArray): String? {
        return try {
            // Nombre único para el archivo
            val fileName = "$userId-${UUID.randomUUID()}.jpg"

            // Subir la imagen (versión actualizada)
            client.storage.from(bucketName).upload(
                path = fileName,
                bytes = imageBytes,  // Actualizado: ahora usa 'bytes' en lugar de 'data'
                upsert = true
            )

            // Obtener la URL pública
            val publicUrl = client.storage.from(bucketName).publicUrl(fileName)
            Log.d("ProfileImageDao", "Imagen subida correctamente: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e("ProfileImageDao", "Error subiendo imagen de perfil: ${e.message}", e)
            null
        }
    }

    override suspend fun getProfileImageUrl(userId: String): String? {
        return try {
            // Buscar archivos con el prefijo del userId (versión actualizada)
            val files = client.storage.from(bucketName).list(
                path = "",  // Directorio raíz del bucket
                options = io.github.jan.supabase.storage.StorageListOptions(
                    prefix = userId,
                    limit = 1,
                    offset = 0,
                    sortBy = null
                )
            )

            if (files.isNotEmpty()) {
                // Devolver la URL pública del primer archivo encontrado
                val publicUrl = client.storage.from(bucketName).publicUrl(files.first().name)
                Log.d("ProfileImageDao", "URL de imagen obtenida: $publicUrl")
                publicUrl
            } else {
                Log.d("ProfileImageDao", "No se encontraron imágenes para el usuario $userId")
                null
            }
        } catch (e: Exception) {
            Log.e("ProfileImageDao", "Error obteniendo URL de imagen: ${e.message}", e)
            null
        }
    }

    override suspend fun deleteProfileImage(userId: String): Boolean {
        return try {
            // Buscar archivos con el prefijo del userId (versión actualizada)
            val files = client.storage.from(bucketName).list(
                path = "",  // Directorio raíz del bucket
                options = io.github.jan.supabase.storage.StorageListOptions(
                    prefix = userId
                )
            )

            // Eliminar todos los archivos encontrados
            if (files.isNotEmpty()) {
                client.storage.from(bucketName).delete(files.map { it.name })
                Log.d("ProfileImageDao", "Imágenes eliminadas para el usuario $userId")
                true
            } else {
                Log.d("ProfileImageDao", "No se encontraron imágenes para eliminar")
                false
            }
        } catch (e: Exception) {
            Log.e("ProfileImageDao", "Error eliminando imágenes: ${e.message}", e)
            false
        }
    }
}