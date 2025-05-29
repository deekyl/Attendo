package com.example.attendo.ui.viewmodel.user

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendo.data.dao.interfaces.ProfileImageDao
import com.example.attendo.data.dao.interfaces.UserDao
import com.example.attendo.data.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class UserManagementViewModel(
    private val userDao: UserDao,
    private val profileImageDao: ProfileImageDao
) : ViewModel() {

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _operationResult = MutableStateFlow<OperationResult?>(null)
    val operationResult: StateFlow<OperationResult?> = _operationResult.asStateFlow()

    private val _profileImageUrl = MutableStateFlow<String?>(null)
    val profileImageUrl: StateFlow<String?> = _profileImageUrl.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val users = userDao.getAllUsersIncludingInactive()
                _allUsers.value = users
                Log.d("Attendo", "Usuarios cargados: ${users.size}")
            } catch (e: Exception) {
                Log.e("Attendo", "Error cargando usuarios: ${e.message}", e)
                _operationResult.value = OperationResult.Error("Error cargando usuarios: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectUser(user: User) {
        _selectedUser.value = user
        loadProfileImage(user.userId)
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val success = userDao.updateUser(user)
                if (success) {
                    _selectedUser.value = user
                    loadUsers()
                    _operationResult.value = OperationResult.Success("Usuario actualizado correctamente")
                } else {
                    _operationResult.value = OperationResult.Error("Error al actualizar el usuario")
                }
            } catch (e: Exception) {
                Log.e("Attendo", "Error actualizando usuario: ${e.message}", e)
                _operationResult.value = OperationResult.Error("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createUser(
        fullName: String,
        email: String,
        documentId: String,
        address: String,
        isAdmin: Boolean
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val userId = UUID.randomUUID().toString()

                val newUser = User(
                    userId = userId,
                    fullName = fullName,
                    email = email,
                    documentId = documentId,
                    address = address,
                    isAdmin = isAdmin,
                    isActive = true,
                    profileImage = "",
                    createdAt = java.time.LocalDateTime.now().toString()
                )

                val result = userDao.createUser(newUser)
                if (result != null) {
                    loadUsers() // Recargar lista
                    _operationResult.value = OperationResult.Success("Usuario creado correctamente")
                } else {
                    _operationResult.value = OperationResult.Error("Error al crear el usuario")
                }
            } catch (e: Exception) {
                Log.e("Attendo", "Error creando usuario: ${e.message}", e)
                _operationResult.value = OperationResult.Error("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleUserStatus(userId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val success = userDao.toggleUserStatus(userId, isActive)
                if (success) {
                    _selectedUser.value?.let { user ->
                        if (user.userId == userId) {
                            _selectedUser.value = user.copy(isActive = isActive)
                        }
                    }
                    loadUsers() // Recargar lista
                    val statusText = if (isActive) "activado" else "desactivado"
                    _operationResult.value = OperationResult.Success("Usuario $statusText correctamente")
                } else {
                    _operationResult.value = OperationResult.Error("Error al cambiar el estado del usuario")
                }
            } catch (e: Exception) {
                Log.e("Attendo", "Error cambiando estado de usuario: ${e.message}", e)
                _operationResult.value = OperationResult.Error("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val success = userDao.deleteUser(userId)
                if (success) {
                    if (_selectedUser.value?.userId == userId) {
                        _selectedUser.value = null
                        _profileImageUrl.value = null
                    }
                    loadUsers()
                    _operationResult.value = OperationResult.Success("Usuario eliminado correctamente")
                } else {
                    _operationResult.value = OperationResult.Error("Error al eliminar el usuario")
                }
            } catch (e: Exception) {
                Log.e("Attendo", "Error eliminando usuario: ${e.message}", e)
                _operationResult.value = OperationResult.Error("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadProfileImage(userId: String) {
        viewModelScope.launch {
            try {
                val url = profileImageDao.getProfileImageUrl(userId)
                _profileImageUrl.value = url
                Log.d("Attendo", "Imagen de perfil cargada: $url")
            } catch (e: Exception) {
                Log.e("Attendo", "Error cargando imagen de perfil: ${e.message}", e)
                _profileImageUrl.value = null
            }
        }
    }

    fun uploadProfileImage(userId: String, imageBytes: ByteArray) {
        viewModelScope.launch {
            try {
                _isUploadingImage.value = true
                val url = profileImageDao.uploadProfileImage(userId, imageBytes)
                if (url != null) {
                    _profileImageUrl.value = url
                    Log.d("Attendo", "Imagen de perfil actualizada: $url")
                    _operationResult.value = OperationResult.Success("Imagen actualizada correctamente")
                } else {
                    _operationResult.value = OperationResult.Error("Error al subir la imagen")
                }
            } catch (e: Exception) {
                Log.e("Attendo", "Error subiendo imagen: ${e.message}", e)
                _operationResult.value = OperationResult.Error("Error al subir la imagen: ${e.message}")
            } finally {
                _isUploadingImage.value = false
            }
        }
    }

    fun clearOperationResult() {
        _operationResult.value = null
    }

    fun clearSelectedUser() {
        _selectedUser.value = null
        _profileImageUrl.value = null
    }

    fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex()) && email.isNotEmpty()
    }

    sealed class OperationResult {
        data class Success(val message: String) : OperationResult()
        data class Error(val message: String) : OperationResult()
    }
}