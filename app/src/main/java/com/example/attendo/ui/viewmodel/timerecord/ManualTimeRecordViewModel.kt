package com.example.attendo.ui.viewmodel.timerecord

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendo.data.dao.interfaces.BreakTypeDao
import com.example.attendo.data.dao.interfaces.TimeRecordDao
import com.example.attendo.data.dao.interfaces.UserDao
import com.example.attendo.data.model.attendance.BreakType
import com.example.attendo.data.model.attendance.TimeRecord
import com.example.attendo.data.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ManualTimeRecordViewModel(
    private val timeRecordDao: TimeRecordDao,
    private val breakTypeDao: BreakTypeDao,
    private val userDao: UserDao,
    private val adminUserId: String
) : ViewModel() {

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _breakTypes = MutableStateFlow<List<BreakType>>(emptyList())
    val breakTypes: StateFlow<List<BreakType>> = _breakTypes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _operationResult = MutableStateFlow<OperationResult?>(null)
    val operationResult: StateFlow<OperationResult?> = _operationResult.asStateFlow()

    // Estados para la pantalla
    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser.asStateFlow()

    // Estado para el último registro del usuario seleccionado
    private val _lastTimeRecord = MutableStateFlow<TimeRecord?>(null)
    val lastTimeRecord: StateFlow<TimeRecord?> = _lastTimeRecord.asStateFlow()

    init {
        viewModelScope.launch {
            loadUsers()
            loadBreakTypes()
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val users = userDao.getAllUsers()
                _allUsers.value = users
                Log.d("Attendo", "Usuarios cargados: ${users.size}")
            } catch (e: Exception) {
                Log.e("Attendo", "Error cargando usuarios: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadBreakTypes() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val breakTypes = breakTypeDao.getAllActiveBreakTypes()
                _breakTypes.value = breakTypes
                Log.d("Attendo", "Tipos de pausa cargados: ${breakTypes.size}")
            } catch (e: Exception) {
                Log.e("Attendo", "Error cargando tipos de pausa: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectUser(user: User) {
        _selectedUser.value = user

        // Al seleccionar un usuario, cargamos su último fichaje
        viewModelScope.launch {
            _lastTimeRecord.value = timeRecordDao.getLastTimeRecord(user.userId)
            Log.d("Attendo", "Último fichaje: ${_lastTimeRecord.value}")
        }
    }

    // Determinar si la entrada es una vuelta de pausa
    fun isReturnFromBreak(): Boolean {
        val lastRecord = _lastTimeRecord.value
        return lastRecord != null && !lastRecord.isEntry && lastRecord.breakTypeId != null
    }

    // Obtener el ID del último tipo de pausa
    fun getLastBreakTypeId(): Int? {
        return _lastTimeRecord.value?.breakTypeId
    }

    fun insertManualRecord(
        userId: String,
        isEntry: Boolean,
        breakTypeId: Int?,
        localDateTime: LocalDateTime,
        location: String?
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val formattedDateTime = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                val record = TimeRecord(
                    userId = userId,
                    time = formattedDateTime,
                    isEntry = isEntry,
                    breakTypeId = breakTypeId,
                    location = location,
                    isManual = true
                )

                Log.d("Attendo", "Insertando fichaje: $record")
                val result = timeRecordDao.insertTimeRecord(record)

                if (result != null) {
                    // Actualizar el último fichaje después de guardar
                    _lastTimeRecord.value = result
                    _operationResult.value = OperationResult.Success("Fichaje manual creado correctamente")
                } else {
                    _operationResult.value = OperationResult.Error("Error al crear el fichaje manual")
                }
            } catch (e: Exception) {
                Log.e("Attendo", "Error insertando fichaje manual: ${e.message}", e)
                _operationResult.value = OperationResult.Error("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearOperationResult() {
        _operationResult.value = null
    }

    sealed class OperationResult {
        data class Success(val message: String) : OperationResult()
        data class Error(val message: String) : OperationResult()
    }
}