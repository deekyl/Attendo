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
            client.postgrest
                .from("break_types")
                .select {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<BreakType>()
        } catch (e: Exception) {
            Log.e("attendo", "Error obteniendo tipos de pausa activos: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getBreakTypeById(breakId: Int): BreakType? {
        return try {
            client.postgrest
                .from("break_types")
                .select {
                    filter {
                        eq("break_id", breakId)
                    }
                }
                .decodeSingle<BreakType>()
        } catch (e: Exception) {
            Log.e("attendo", "Error obteniendo tipo de pausa: ${e.message}", e)
            null
        }
    }

}