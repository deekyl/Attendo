package com.example.attendo.ui.viewmodel.breaktype

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendo.data.dao.interfaces.BreakTypeDao
import com.example.attendo.data.model.attendance.BreakType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BreakTypeViewModel(
    private val breakTypeDao: BreakTypeDao
) : ViewModel() {

    private val _breakTypes = MutableStateFlow<List<BreakType>>(emptyList())
    val breakTypes: StateFlow<List<BreakType>> = _breakTypes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _operationResult = MutableStateFlow<OperationResult?>(null)
    val operationResult: StateFlow<OperationResult?> = _operationResult.asStateFlow()

    init {
        loadBreakTypes()
    }

    fun loadBreakTypes() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = breakTypeDao.getAllBreakTypes()
                _breakTypes.value = result
            } catch (e: Exception) {
                Log.e("Attendo", "Error cargando tipos de pausa: ${e.message}", e)
                _operationResult.value = OperationResult.Error("Error cargando tipos de pausa: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createBreakType(description: String, computesAs: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val newBreakType = BreakType(
                    description = description,
                    computesAs = computesAs,
                    isActive = true
                )

                val result = breakTypeDao.insertBreakType(newBreakType)

                if (result != null) {
                    _operationResult.value = OperationResult.Success("Tipo de pausa creado correctamente")
                    loadBreakTypes() // Recargamos la lista
                } else {
                    _operationResult.value = OperationResult.Error("Error al crear el tipo de pausa")
                }
            } catch (e: Exception) {
                Log.e("Attendo", "Error creando tipo de pausa: ${e.message}", e)
                _operationResult.value = OperationResult.Error("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateBreakType(breakType: BreakType) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val result = breakTypeDao.updateBreakType(breakType)

                if (result != null) {
                    _operationResult.value = OperationResult.Success("Tipo de pausa actualizado correctamente")
                    loadBreakTypes() // Recargamos la lista
                } else {
                    _operationResult.value = OperationResult.Error("Error al actualizar el tipo de pausa")
                }
            } catch (e: Exception) {
                Log.e("Attendo", "Error actualizando tipo de pausa: ${e.message}", e)
                _operationResult.value = OperationResult.Error("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleBreakTypeStatus(breakId: Int?, isActive: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val success = breakTypeDao.toggleBreakTypeStatus(breakId, isActive)

                if (success) {
                    val statusText = if (isActive) "activado" else "desactivado"
                    _operationResult.value = OperationResult.Success("Tipo de pausa $statusText correctamente")
                    loadBreakTypes() // Recargamos la lista
                } else {
                    _operationResult.value = OperationResult.Error("Error al cambiar el estado del tipo de pausa")
                }
            } catch (e: Exception) {
                Log.e("Attendo", "Error cambiando estado de tipo de pausa: ${e.message}", e)
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