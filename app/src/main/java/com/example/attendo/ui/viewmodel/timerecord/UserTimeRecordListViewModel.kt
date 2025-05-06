package com.example.attendo.ui.viewmodel.timerecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendo.data.dao.interfaces.BreakTypeDao
import com.example.attendo.data.dao.interfaces.TimeRecordDao
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

class TimeRecordListViewModel(
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

    // Cargamos los registros inicialmente
    init {
        loadTimeRecords()
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
                // Lógica para obtener todos los registros y filtrarlos en memoria
                val allRecords = getAllTimeRecordsForUser()
                _filteredRecords.value = filterRecords(allRecords)
            } catch (e: Exception) {
                // Manejar error
                _filteredRecords.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Función auxiliar para obtener todos los registros del usuario
    private suspend fun getAllTimeRecordsForUser(): List<TimeRecord> {
        // Esta es una implementación simplificada.
        // En un caso real, deberíamos tener una función en el DAO para obtener registros con filtros

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

    // Función para filtrar los registros según los criterios
    private fun filterRecords(records: List<TimeRecord>): List<TimeRecord> {
        val currentFilter = _filter.value

        return records.filter { record ->
            // Filtrar por tipo de acción
            val actionMatch = when (currentFilter.actionType) {
                TimeRecordFilter.ActionType.ALL -> true
                TimeRecordFilter.ActionType.ENTRY -> record.isEntry && record.breakTypeId == null
                TimeRecordFilter.ActionType.EXIT -> !record.isEntry && record.breakTypeId == null
                TimeRecordFilter.ActionType.BREAK -> record.breakTypeId != null
            }

            // Filtrar por fecha inicio
            val startDateMatch = if (currentFilter.startDate != null) {
                try {
                    val recordDate = parseDateTime(record.time).toLocalDate()
                    recordDate.isEqual(currentFilter.startDate) || recordDate.isAfter(currentFilter.startDate)
                } catch (e: DateTimeParseException) {
                    true // Si no podemos parsear la fecha, lo incluimos igual
                }
            } else {
                true
            }

            // Filtrar por fecha fin
            val endDateMatch = if (currentFilter.endDate != null) {
                try {
                    val recordDate = parseDateTime(record.time).toLocalDate()
                    recordDate.isEqual(currentFilter.endDate) || recordDate.isBefore(currentFilter.endDate)
                } catch (e: DateTimeParseException) {
                    true // Si no podemos parsear la fecha, lo incluimos igual
                }
            } else {
                true
            }

            actionMatch && startDateMatch && endDateMatch
        }
            .let { filteredList ->
                // Ordenar según el criterio seleccionado
                if (currentFilter.ascending) {
                    filteredList.sortedBy { it.time }
                } else {
                    filteredList.sortedByDescending { it.time }
                }
            }
            .take(currentFilter.limit) // Limitar el número de resultados
    }

    // Función auxiliar para parsear fechas
    private fun parseDateTime(dateTimeStr: String): LocalDateTime {
        return try {
            LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } catch (e: DateTimeParseException) {
            try {
                LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (e2: DateTimeParseException) {
                // Formato por defecto
                LocalDateTime.parse(dateTimeStr)
            }
        }
    }
}