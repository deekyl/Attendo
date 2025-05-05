package com.example.attendo.data.dao.interfaces

import com.example.attendo.data.model.attendance.BreakType

interface BreakTypeDao {
    suspend fun getAllActiveBreakTypes(): List<BreakType>
    suspend fun getBreakTypeById(breakId: Int): BreakType?
}