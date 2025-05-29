package com.example.attendo.ui.viewmodel.timerecord

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendo.data.dao.interfaces.BreakTypeDao
import com.example.attendo.data.dao.interfaces.TimeRecordDao
import com.example.attendo.data.model.attendance.BreakType
import com.example.attendo.data.model.attendance.TimeRecord
import com.example.attendo.data.model.attendance.TimeRecordFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class UserTimeRecordListViewModel(
    private val timeRecordDao: TimeRecordDao,
    private val breakTypeDao: BreakTypeDao,
    private val userId: String
) : ViewModel() {

    private val _filteredRecords = MutableStateFlow<List<TimeRecord>>(emptyList())
    val filteredRecords: StateFlow<List<TimeRecord>> = _filteredRecords.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _filter = MutableStateFlow(TimeRecordFilter(userId = userId))
    val filter: StateFlow<TimeRecordFilter> = _filter.asStateFlow()

    private val _breakTypesMap = MutableStateFlow<Map<Int, BreakType>>(emptyMap())

    init {
        loadBreakTypes()
        loadTimeRecords()
    }

    private fun loadBreakTypes() {
        viewModelScope.launch {
            try {
                Log.d("UserTimeRecordListVM", "Cargando tipos de pausa...")
                val breakTypes = breakTypeDao.getAllActiveBreakTypes()

                Log.d("UserTimeRecordListVM", "Tipos de pausa cargados: ${breakTypes.size}")
                breakTypes.forEach {
                    Log.d("UserTimeRecordListVM", "Break ID: ${it.breakId}, Desc: ${it.description}")
                }

                _breakTypesMap.value = breakTypes.associateBy { it.breakId!! }
            } catch (e: Exception) {
                Log.e("UserTimeRecordListVM", "Error cargando tipos de pausa: ${e.message}", e)
            }
        }
    }

    // Obtener la descripción de un tipo de pausa
    fun getBreakTypeDescription(breakId: Int): String {
        val result = _breakTypesMap.value[breakId]?.description ?: "Pausa $breakId"
        Log.d("UserTimeRecordListVM", "Solicitando descripción para ID $breakId: $result")
        return result
    }

    // Función para actualizar el filtro
    suspend fun updateFilter(newFilter: TimeRecordFilter) {
        _filter.value = newFilter
    }

    // Función para cargar los registros según los filtros
    fun loadTimeRecords() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val allRecords = getAllTimeRecordsForUser()
                _filteredRecords.value = filterRecords(allRecords)
            } catch (_: Exception) {
                _filteredRecords.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Función auxiliar para obtener todos los registros del usuario
    private suspend fun getAllTimeRecordsForUser(): List<TimeRecord> {
        // Configurar fechas por defecto si no están establecidas
        val currentFilter = _filter.value
        val startDate = currentFilter.startDate?.toString() ?: LocalDate.now().withDayOfMonth(1).toString()
        val endDate = currentFilter.endDate?.toString() ?: LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1).toString()

        return try {
            timeRecordDao.getTimeRecordsByDateRange(userId, startDate, endDate)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun filterRecords(records: List<TimeRecord>): List<TimeRecord> {
        val currentFilter = _filter.value

        return records.filter { record ->
            val actionMatch = when (currentFilter.actionType) {
                TimeRecordFilter.ActionType.ALL -> true
                TimeRecordFilter.ActionType.ENTRY -> record.isEntry && record.breakTypeId == null
                TimeRecordFilter.ActionType.EXIT -> !record.isEntry && record.breakTypeId == null
                TimeRecordFilter.ActionType.BREAK -> record.breakTypeId != null
            }

            val startDateMatch = if (currentFilter.startDate != null) {
                try {
                    val recordDate = parseDateTime(record.time).toLocalDate()
                    recordDate.isEqual(currentFilter.startDate) || recordDate.isAfter(currentFilter.startDate)
                } catch (_: DateTimeParseException) {
                    true
                }
            } else {
                true
            }

            val endDateMatch = if (currentFilter.endDate != null) {
                try {
                    val recordDate = parseDateTime(record.time).toLocalDate()
                    recordDate.isEqual(currentFilter.endDate) || recordDate.isBefore(currentFilter.endDate)
                } catch (e: DateTimeParseException) {
                    true
                }
            } else {
                true
            }

            actionMatch && startDateMatch && endDateMatch
        }
            .let { filteredList ->
                if (currentFilter.ascending) {
                    filteredList.sortedBy { it.time }
                } else {
                    filteredList.sortedByDescending { it.time }
                }
            }
            .take(currentFilter.limit)
    }

  private fun parseDateTime(dateTimeStr: String): LocalDateTime {
        return try {
            LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (_: DateTimeParseException) {
                LocalDateTime.parse(dateTimeStr)
            }
        }
    }
}