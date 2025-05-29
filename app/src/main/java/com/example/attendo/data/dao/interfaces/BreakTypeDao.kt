package com.example.attendo.data.dao.interfaces

import com.example.attendo.data.model.attendance.BreakType

interface BreakTypeDao {
    suspend fun getAllBreakTypes(): List<BreakType>
    suspend fun getAllActiveBreakTypes(): List<BreakType>
    suspend fun getBreakTypeById(breakId: Int): BreakType?
    suspend fun insertBreakType(breakType: BreakType): BreakType?
    suspend fun updateBreakType(breakType: BreakType): BreakType?
    suspend fun toggleBreakTypeStatus(breakId: Int?, isActive: Boolean): Boolean
}