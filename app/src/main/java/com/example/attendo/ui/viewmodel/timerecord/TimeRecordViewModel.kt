package com.example.attendo.ui.viewmodel.timerecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendo.data.dao.interfaces.BreakTypeDao
import com.example.attendo.data.dao.interfaces.TimeRecordDao
import com.example.attendo.data.model.attendance.BreakType
import com.example.attendo.data.model.attendance.TimeRecord
import com.example.attendo.data.model.attendance.TimeRecordState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TimeRecordViewModel(
    private val timeRecordDao: TimeRecordDao,
    private val breakTypeDao: BreakTypeDao,
    private val userId: String
) : ViewModel() {

    private val _timeRecordState = MutableStateFlow<TimeRecordState>(TimeRecordState.Loading)
    val timeRecordState: StateFlow<TimeRecordState> = _timeRecordState.asStateFlow()

    private val _todayRecords = MutableStateFlow<List<TimeRecord>>(emptyList())
    val todayRecords: StateFlow<List<TimeRecord>> = _todayRecords.asStateFlow()

    private val _breakTypes = MutableStateFlow<List<BreakType>>(emptyList())
    val breakTypes: StateFlow<List<BreakType>> = _breakTypes.asStateFlow()

    init {
        loadCurrentStatus()
        loadTodayRecords()
        loadBreakTypes()
    }

    private fun loadCurrentStatus() {
        viewModelScope.launch {
            _timeRecordState.value = TimeRecordState.Loading

            try {
                val lastRecord = timeRecordDao.getLastTimeRecord(userId)

                _timeRecordState.value = when {
                    lastRecord == null -> TimeRecordState.CheckedOut
                    lastRecord.isEntry -> TimeRecordState.CheckedIn
                    lastRecord.breakTypeId != null -> TimeRecordState.OnBreak(lastRecord.breakTypeId)
                    else -> TimeRecordState.CheckedOut
                }
            } catch (e: Exception) {
                _timeRecordState.value = TimeRecordState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    private fun loadTodayRecords() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                val records = timeRecordDao.getTimeRecordsByDay(userId, today)
                _todayRecords.value = records
            } catch (e: Exception) {
            }
        }
    }

    private fun loadBreakTypes() {
        viewModelScope.launch {
            try {
                _breakTypes.value = breakTypeDao.getAllActiveBreakTypes()
            } catch (e: Exception) {
                // Error silencioso, podríamos mostrar un error en la UI si es necesario
            }
        }
    }

    fun checkIn(location: String? = null) {
        createTimeRecord(true, null, location)
    }

    fun checkOut(location: String? = null) {
        createTimeRecord(false, null, location)
    }

    fun startBreak(breakTypeId: Int, location: String? = null) {
        createTimeRecord(false, breakTypeId, location)
    }

    fun endBreak(location: String? = null) {
        createTimeRecord(true, null, location)
    }

    fun getBreakTypeDescription(breakId: Int): String {
        return _breakTypes.value.find { it.breakId == breakId }?.description ?: "Descanso"
    }

    private fun createTimeRecord(isEntry: Boolean, breakTypeId: Int?, location: String?) {
        viewModelScope.launch {
            _timeRecordState.value = TimeRecordState.Loading

            try {
                val now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
                val record = TimeRecord(
                    userId = userId,
                    time = now,
                    isEntry = isEntry,
                    breakTypeId = breakTypeId,
                    location = location,
                    isManual = false // Asumimos que es un registro automático
                )

                timeRecordDao.insertTimeRecord(record)
                loadCurrentStatus()
                loadTodayRecords()
            } catch (e: Exception) {
                _timeRecordState.value = TimeRecordState.Error(e.message ?: "Error al registrar fichaje")
            }
        }
    }
}
