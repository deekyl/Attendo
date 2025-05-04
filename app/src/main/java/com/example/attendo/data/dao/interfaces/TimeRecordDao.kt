package com.example.attendo.data.dao.interfaces

import com.example.attendo.data.model.attendance.TimeRecord

interface TimeRecordDao {
    suspend fun getLastTimeRecord(userId: String): TimeRecord?
    suspend fun getTimeRecordsByDay(userId: String, date: String): List<TimeRecord>
    suspend fun insertTimeRecord(record: TimeRecord): TimeRecord?
}