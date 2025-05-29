package com.example.attendo.data.dao.implementation

import android.util.Log
import com.example.attendo.data.dao.interfaces.TimeRecordDao
import com.example.attendo.data.model.attendance.TimeRecord
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class TimeRecordImplSupabase(
    private val client: SupabaseClient
) : TimeRecordDao {

    override suspend fun getLastTimeRecord(userId: String): TimeRecord? {
        return try {
            val response = client.postgrest
                .from("time_records")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<TimeRecord>()

            if (response.isNotEmpty()) response.first() else null
        } catch (e: Exception) {
            Log.e("attendo", "Error obteniendo los registros de fichaje: ${e.message}", e)
            null
        }
    }

    override suspend fun getTimeRecordsByDay(
        userId: String,
        date: String
    ): List<TimeRecord> {
        return try {
            val startOfDay = "${date}T00:00:00"
            val endOfDay = "${date}T23:59:59"

            client.postgrest
                .from("time_records")
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("time", startOfDay)
                        lte("time", endOfDay)
                    }
                    order("time", Order.DESCENDING)
                }
                .decodeList<TimeRecord>()
        } catch (e: Exception) {
            Log.e("attendo", "Error obteniendo ficjaes por día: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun insertTimeRecord(record: TimeRecord): TimeRecord? {
        return try {
            client.postgrest
                .from("time_records")
                .insert(record) {
                    select()
                }
                .decodeSingle<TimeRecord>()
        } catch (e: Exception) {
            Log.e("attendo", "Error al insertar fichaje: ${e.message}", e)
            null
        }
    }

    override suspend fun getTimeRecordsByDateRange(
        userId: String,
        startDate: String,
        endDate: String
    ): List<TimeRecord> {
        return try {
            val startDateTime = "${startDate}T00:00:00"
            val endDateTime = "${endDate}T23:59:59"

            client.postgrest
                .from("time_records")
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("time", startDateTime)
                        lte("time", endDateTime)
                    }
                    order("time", Order.DESCENDING)
                }
                .decodeList<TimeRecord>()
        } catch (e: Exception) {
            Log.e("attendo", "Error obteniendo fichajes por rango de fechas: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getAllTimeRecordsByDateRange(
        startDate: String,
        endDate: String
    ): List<TimeRecord> {
        return try {
            val startDateTime = "${startDate}T00:00:00"
            val endDateTime = "${endDate}T23:59:59"

            client.postgrest
                .from("time_records")
                .select {
                    filter {
                        gte("time", startDateTime)
                        lte("time", endDateTime)
                    }
                    order("time", Order.DESCENDING)
                }
                .decodeList<TimeRecord>()
        } catch (e: Exception) {
            Log.e("attendo", "Error obteniendo todos los fichajes por rango de fechas: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun updateTimeRecord(record: TimeRecord): TimeRecord? {
        return try {
            client.postgrest
                .from("time_records")
                .update(
                    {
                        set("time", record.time)
                        set("is_entry", record.isEntry)
                        set("break_type_id", record.breakTypeId)
                        set("location", record.location)
                        set("is_manual", true)
                    }
                ) {
                    filter {
                        eq("record_id", record.recordId!!)
                    }
                    select()
                }
                .decodeSingle<TimeRecord>()
        } catch (e: Exception) {
            Log.e("attendo", "Error al actualizar fichaje: ${e.message}", e)
            null
        }
    }
}
