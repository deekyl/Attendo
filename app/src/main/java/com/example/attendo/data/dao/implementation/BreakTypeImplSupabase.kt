// Asegúrate de que tu implementación de BreakTypeDao tenga esta estructura:
package com.example.attendo.data.dao.implementation

import android.util.Log
import com.example.attendo.data.dao.interfaces.BreakTypeDao
import com.example.attendo.data.model.attendance.BreakType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class BreakTypeImplSupabase(
    private val client: SupabaseClient
) : BreakTypeDao {

    override suspend fun getAllActiveBreakTypes(): List<BreakType> {
        return try {
            Log.d("BreakTypeDao", "Obteniendo tipos de pausa activos")
            val result = client.postgrest
                .from("break_types")
                .select {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<BreakType>()

            // Log para debug
            Log.d("BreakTypeDao", "Tipos de pausa obtenidos: ${result.size}")
            result.forEach {
                Log.d("BreakTypeDao", "Pausa ID: ${it.breakId}, Desc: ${it.description}")
            }

            result
        } catch (e: Exception) {
            Log.e("BreakTypeDao", "Error obteniendo tipos de pausa activos: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getBreakTypeById(breakId: Int): BreakType? {
        return try {
            val result = client.postgrest
                .from("break_types")
                .select {
                    filter {
                        eq("break_id", breakId)
                    }
                }
                .decodeSingle<BreakType>()

            // Log para debug
            Log.d("BreakTypeDao", "Obtención de pausa por ID: ${breakId}, Desc: ${result.description}")

            result
        } catch (e: Exception) {
            Log.e("BreakTypeDao", "Error obteniendo tipo de pausa con ID ${breakId}: ${e.message}", e)
            null
        }
    }
}