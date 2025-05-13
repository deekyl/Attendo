// app/src/main/java/com/example/attendo/ui/viewmodel/timerecord/AdminTimeRecordListViewModel.kt
package com.example.attendo.ui.viewmodel.timerecord

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendo.data.dao.interfaces.BreakTypeDao
import com.example.attendo.data.dao.interfaces.TimeRecordDao
import com.example.attendo.data.dao.interfaces.UserDao
import com.example.attendo.data.model.attendance.BreakType
import com.example.attendo.data.model.attendance.TimeRecord
import com.example.attendo.data.model.attendance.TimeRecordFilter
import com.example.attendo.data.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class AdminTimeRecordListViewModel(
    private val timeRecordDao: TimeRecordDao,
    private val breakTypeDao: BreakTypeDao,
    private val userDao: UserDao,
    private val adminUserId: String // Recibimos el ID del admin
) : ViewModel() {

    private val _filteredRecords = MutableStateFlow<List<TimeRecord>>(emptyList())
    val filteredRecords: StateFlow<List<TimeRecord>> = _filteredRecords.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Inicializamos el filtro con el ID del administrador
    private val _filter = MutableStateFlow(TimeRecordFilter(userId = adminUserId))
    val filter: StateFlow<TimeRecordFilter> = _filter.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser.asStateFlow()

    private val _breakTypesMap = MutableStateFlow<Map<Int, BreakType>>(emptyMap())
    val breakTypesMap: StateFlow<Map<Int, BreakType>> = _breakTypesMap.asStateFlow()

    // Caché de usuarios para mostrar nombres
    private val userCache = mutableMapOf<String, User>()

    // Inicialización con el admin
    suspend fun initialize(adminUser: User) {
        try {
            // Cargamos primero los tipos de pausa y otros datos que no involucren al usuario
            loadBreakTypes()

            // Guardamos el usuario admin en la caché y actualizamos el estado
            userCache[adminUser.userId] = adminUser
            _selectedUser.value = adminUser
            _filter.value = _filter.value.copy(userId = adminUser.userId)

            // Finalmente cargamos lo más "pesado": usuarios y registros
            viewModelScope.launch { loadUsers() }
            viewModelScope.launch { loadTimeRecords() }
        } catch (e: Exception) {
            Log.e("Attendo", "Error en initialize: ${e.message}", e)
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val users = userDao.getAllUsers()
                _allUsers.value = users

                // Actualizamos la caché de usuarios para consulta rápida
                users.forEach { user ->
                    userCache[user.userId] = user
                }
            } catch (e: Exception) {
                // Capturamos también CancellationException
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("Attendo", "Carga de usuarios cancelada", e)
                } else {
                    Log.e("Attendo", "Error cargando usuarios: ${e.message}", e)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Función para seleccionar un usuario específico
    suspend fun selectUser(user: User) {
        _selectedUser.value = user

        // Actualizamos el filtro con el ID del usuario seleccionado
        updateFilter(_filter.value.copy(userId = user.userId))

        // Recargamos los registros
        loadTimeRecords()
    }

    // Función para actualizar el filtro
    suspend fun updateFilter(newFilter: TimeRecordFilter) {
        _filter.value = newFilter
    }

    // Función para cargar los registros según los filtros
    fun loadTimeRecords() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val userId = _selectedUser.value?.userId ?: ""
                if (userId.isNotEmpty()) {
                    val records = getAllTimeRecordsForUser(userId)
                    _filteredRecords.value = filterRecords(records)
                } else {
                    _filteredRecords.value = emptyList()
                }
            } catch (e: Exception) {
                // Capturamos también CancellationException
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("Attendo", "Carga de registros cancelada", e)
                } else {
                    Log.e("Attendo", "Error cargando registros: ${e.message}", e)
                }
                _filteredRecords.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }


    // Obtener registros para un usuario específico
    private suspend fun getAllTimeRecordsForUser(userId: String): List<TimeRecord> {
        val currentFilter = _filter.value
        val startDate =
            currentFilter.startDate?.toString() ?: LocalDate.now().withDayOfMonth(1).toString()
        val endDate =
            currentFilter.endDate?.toString() ?: LocalDate.now().plusMonths(1).withDayOfMonth(1)
                .minusDays(1).toString()

        return try {
            timeRecordDao.getTimeRecordsByDateRange(userId, startDate, endDate)
        } catch (e: Exception) {
            Log.e("Attendo", "Error obteniendo registros: ${e.message}", e)
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

    private fun loadBreakTypes() {
        viewModelScope.launch {
            try {
                Log.d("Attendo", "Cargando tipos de pausa...")
                val breakTypes = breakTypeDao.getAllActiveBreakTypes()

                _breakTypesMap.value = breakTypes.associateBy { it.breakId }

                Log.d("Attendo", "Tipos de pausa cargados: ${breakTypes.size}")
                breakTypes.forEach {
                    Log.d("Attendo", "Break ID: ${it.breakId}, Desc: ${it.description}")
                }
            } catch (e: Exception) {
                Log.e("Attendo", "Error cargando tipos de pausa: ${e.message}", e)
            }
        }
    }

    // Función para obtener el nombre de un usuario a partir de su ID
    fun getUserName(userId: String): String {
        return try {
            userCache[userId]?.fullName ?: "Usuario"
        } catch (e: Exception) {
            "Usuario"
        }
    }
}
