package com.example.attendo.ui.viewmodel.timerecord

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendo.data.dao.interfaces.BreakTypeDao
import com.example.attendo.data.dao.interfaces.TimeRecordDao
import com.example.attendo.data.model.attendance.BreakType
import com.example.attendo.data.model.attendance.TimeRecord
import com.example.attendo.data.model.attendance.TimeRecordState
import com.example.attendo.utils.AttendoLocationManager
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
    private val userId: String,
    private val context: Context
) : ViewModel() {

    private val _timeRecordState = MutableStateFlow<TimeRecordState>(TimeRecordState.Loading)
    val timeRecordState: StateFlow<TimeRecordState> = _timeRecordState.asStateFlow()

    private val _todayRecords = MutableStateFlow<List<TimeRecord>>(emptyList())
    val todayRecords: StateFlow<List<TimeRecord>> = _todayRecords.asStateFlow()

    private val _breakTypes = MutableStateFlow<List<BreakType>>(emptyList())
    val breakTypes: StateFlow<List<BreakType>> = _breakTypes.asStateFlow()

    private val _isGettingLocation = MutableStateFlow(false)
    val isGettingLocation: StateFlow<Boolean> = _isGettingLocation.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    private val locationManager = AttendoLocationManager(context)

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
            } catch (_: Exception) {
            }
        }
    }

    private fun loadBreakTypes() {
        viewModelScope.launch {
            try {
                _breakTypes.value = breakTypeDao.getAllActiveBreakTypes()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun getCurrentLocationString(): String? {
        return try {
            _isGettingLocation.value = true
            _locationError.value = null

            if (!locationManager.hasLocationPermission()) {
                _locationError.value = "Se requieren permisos de ubicación"
                return null
            }

            if (!locationManager.isLocationEnabled()) {
                _locationError.value = "Los servicios de ubicación están deshabilitados"
                return null
            }

            val result = locationManager.getCurrentLocation()
            result.fold(
                onSuccess = { locationData ->
                    locationManager.formatLocationForDisplay(locationData)
                },
                onFailure = { exception ->
                    _locationError.value = "Error obteniendo ubicación: ${exception.message}"
                    null
                }
            )
        } catch (e: Exception) {
            _locationError.value = "Error inesperado: ${e.message}"
            null
        } finally {
            _isGettingLocation.value = false
        }
    }

    fun checkIn() {
        createTimeRecordWithLocation(isEntry = true, breakTypeId = null)
    }

    fun checkOut() {
        createTimeRecordWithLocation(isEntry = false, breakTypeId = null)
    }

    fun startBreak(breakTypeId: Int) {
        createTimeRecordWithLocation(isEntry = true, breakTypeId = breakTypeId)
    }

    suspend fun endBreak() {
        try {
            val lastRecord = timeRecordDao.getLastTimeRecord(userId)
            val breakTypeId = lastRecord?.breakTypeId
            createTimeRecordWithLocation(isEntry = false, breakTypeId = breakTypeId)
        } catch  (e: Exception) {
            _timeRecordState.value = TimeRecordState.Error(e.message ?: "Error al registrar fichaje")
        }

    }

    fun checkInWithLocation(customLocation: String?) {
        createTimeRecord(isEntry = true, breakTypeId = null, location = customLocation)
    }

    fun checkOutWithLocation(customLocation: String?) {
        createTimeRecord(isEntry = false, breakTypeId = null, location = customLocation)
    }

    fun startBreakWithLocation(breakTypeId: Int, customLocation: String?) {
        createTimeRecord(isEntry = false, breakTypeId = breakTypeId, location = customLocation)
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
                    isManual = false
                )

                timeRecordDao.insertTimeRecord(record)
                loadCurrentStatus()
                loadTodayRecords()
            } catch (e: Exception) {
                _timeRecordState.value = TimeRecordState.Error(e.message ?: "Error al registrar fichaje")
            }
        }
    }

    private fun createTimeRecordWithLocation(isEntry: Boolean, breakTypeId: Int?) {
        viewModelScope.launch {
            _timeRecordState.value = TimeRecordState.Loading

            try {
                val location = getCurrentLocationString()
                createTimeRecord(isEntry, breakTypeId, location)
            } catch (e: Exception) {
                _timeRecordState.value = TimeRecordState.Error(e.message ?: "Error al registrar fichaje")
            }
        }
    }

    fun clearLocationError() {
        _locationError.value = null
    }

    fun hasLocationPermission(): Boolean {
        return locationManager.hasLocationPermission()
    }

    fun isLocationEnabled(): Boolean {
        return locationManager.isLocationEnabled()
    }
}
