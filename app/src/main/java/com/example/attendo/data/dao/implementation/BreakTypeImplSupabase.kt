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

    override suspend fun getAllBreakTypes(): List<BreakType> {
        return try {
            Log.d("Attendo", "Obteniendo todos los tipos de pausa")
            val result = client.postgrest
                .from("break_types")
                .select()
                .decodeList<BreakType>()

            Log.d("BreakTypeDao", "Tipos de pausa obtenidos: ${result.size}")
            result
        } catch (e: Exception) {
            Log.e("BreakTypeDao", "Error obteniendo tipos de pausa: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getAllActiveBreakTypes(): List<BreakType> {
        return try {
            Log.d("Attendo", "Obteniendo tipos de pausa activos")
            val result = client.postgrest
                .from("break_types")
                .select {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<BreakType>()


            Log.d("Attendo", "Tipos de pausa obtenidos: ${result.size}")
            result.forEach {
                Log.d("BreakTypeDao", "Pausa ID: ${it.breakId}, Desc: ${it.description}")
            }

            result
        } catch (e: Exception) {
            Log.e("Attendo", "Error obteniendo tipos de pausa activos: ${e.message}", e)
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
            Log.d("Attendo", "Obtención de pausa por ID: ${breakId}, Desc: ${result.description}")

            result
        } catch (e: Exception) {
            Log.e("Attendo", "Error obteniendo tipo de pausa con ID ${breakId}: ${e.message}", e)
            null
        }
    }

    override suspend fun insertBreakType(breakType: BreakType): BreakType? {
        return try {
            val result = client.postgrest
                .from("break_types")
                .insert(breakType) {
                    select()
                }
                .decodeSingle<BreakType>()

            Log.d("Attendo", "Tipo de pausa insertado: ${result.description}")
            result
        } catch (e: Exception) {
            Log.e("Attendo", "Error insertando tipo de pausa: ${e.message}", e)
            null
        }
    }

    override suspend fun updateBreakType(breakType: BreakType): BreakType? {
        return try {
            val result = client.postgrest
                .from("break_types")
                .update({
                    set("description", breakType.description)
                    set("computes_as_work_time", breakType.computesAs)
                    set("is_active", breakType.isActive)
                }) {
                    filter {
                        eq("break_id", breakType.breakId)
                    }
                    select()
                }
                .decodeSingle<BreakType>()

            Log.d("Attendo", "Tipo de pausa actualizado: ${result.description}")
            result
        } catch (e: Exception) {
            Log.e("Attendo", "Error actualizando tipo de pausa: ${e.message}", e)
            null
        }
    }

    override suspend fun toggleBreakTypeStatus(breakId: Int, isActive: Boolean): Boolean {
        return try {
            client.postgrest
                .from("break_types")
                .update({
                    set("is_active", isActive)
                }) {
                    filter {
                        eq("break_id", breakId)
                    }
                }

            Log.d("Attendo", "Estado de tipo de pausa actualizado. ID: $breakId, Activo: $isActive")
            true
        } catch (e: Exception) {
            Log.e("Attendo", "Error actualizando estado de tipo de pausa: ${e.message}", e)
            false
        }
    }
}