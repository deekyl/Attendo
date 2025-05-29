package com.example.attendo.ui.viewmodel.user

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendo.data.dao.interfaces.ProfileImageDao
import com.example.attendo.data.dao.interfaces.UserDao
import com.example.attendo.data.model.user.User
import com.example.attendo.data.repositories.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class UserManagementViewModel(
    private val userDao: UserDao,
    private val profileImageDao: ProfileImageDao,
    private val authRepository: AuthRepository
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

    private val _userProfileImages = MutableStateFlow<Map<String, String>>(emptyMap())
    val userProfileImages: StateFlow<Map<String, String>> = _userProfileImages.asStateFlow()

    init {
        loadUsers()
    }

    fun refreshUsers(){
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val users = userDao.getAllUsersIncludingInactive()
                _allUsers.value = users

                loadAllProfileImages(users)

                Log.d("Attendo", "Usuarios cargados: ${users.size}")
            } catch (e: Exception) {
                Log.e("Attendo", "Error cargando usuarios: ${e.message}", e)
                _operationResult.value = OperationResult.Error("Error cargando usuarios: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadAllProfileImages(users: List<User>) {
        viewModelScope.launch {
            try {
                val imageMap = mutableMapOf<String, String>()
                users.forEach { user ->
                    try {
                        val imageUrl = profileImageDao.getProfileImageUrl(user.userId)
                        if (imageUrl != null) {
                            imageMap[user.userId] = imageUrl
                        }
                    } catch (e: Exception) {
                        Log.e("Attendo", "Error cargando imagen para usuario ${user.userId}: ${e.message}")
                    }
                }

                _userProfileImages.value = imageMap
                Log.d("Attendo", "Imágenes de perfil cargadas: ${imageMap.size}")
            } catch (e: Exception) {
                Log.e("Attendo", "Error general cargando imágenes: ${e.message}", e)
            }
        }
    }

    fun getProfileImageUrl(userId: String): String? {
        return _userProfileImages.value[userId]
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

                Log.d("Attendo", "Iniciando creación de usuario con email: $email")

                val authResult = authRepository.signUp(email, "123456")

                authResult.fold(
                    onSuccess = { userInfo ->
                        Log.d("Attendo", "Usuario creado en Auth con ID: ${userInfo.id}")

                        val newUser = User(
                            userId = userInfo.id,
                            fullName = fullName,
                            email = email,
                            documentId = documentId,
                            address = address,
                            isAdmin = isAdmin,
                            isActive = true,
                            profileImage = "",
                            createdAt = java.time.LocalDateTime.now().toString()
                        )

                        val userCreated = userDao.createUser(newUser)
                        if (userCreated != null) {
                            Log.d("Attendo", "Usuario creado en la base de datos: ${userCreated.userId}")
                            loadUsers()
                            _operationResult.value = OperationResult.Success(
                                "Usuario creado correctamente. Se ha enviado un email de verificación a $email"
                            )
                        } else {
                            Log.e("Attendo", "Error creando usuario en BD, el usuario ya existe en Auth")
                            _operationResult.value = OperationResult.Error(
                                "Error al crear el usuario en la base de datos"
                            )
                        }
                    },
                    onFailure = { exception ->
                        Log.e("Attendo", "Error creando usuario en Auth: ${exception.message}", exception)

                        val errorMessage = when {
                            exception.message?.contains("email_address_not_authorized") == true ->
                                "El email no está autorizado. Contacta con el administrador."

                            exception.message?.contains("signup_disabled") == true ->
                                "El registro está deshabilitado temporalmente."

                            exception.message?.contains("email_already_exists") == true ||
                                    exception.message?.contains("user_already_exists") == true ->
                                "Ya existe un usuario con este email."

                            exception.message?.contains("invalid_email") == true ->
                                "El formato del email no es válido."

                            exception.message?.contains("weak_password") == true ->
                                "La contraseña es demasiado débil."

                            exception.message?.contains("network") == true ||
                                    exception.message?.contains("connection") == true ->
                                "Error de conexión. Verifica tu conexión a internet."

                            else -> "Error al crear el usuario: ${exception.message}"
                        }

                        _operationResult.value = OperationResult.Error(errorMessage)
                    }
                )
            } catch (e: Exception) {
                Log.e("Attendo", "Error general creando usuario: ${e.message}", e)
                _operationResult.value = OperationResult.Error("Error inesperado: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun generateTemporaryPassword(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*"
        return (1..12)
            .map { chars.random() }
            .joinToString("")
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

                    val currentImages = _userProfileImages.value.toMutableMap()
                    currentImages[userId] = url
                    _userProfileImages.value = currentImages

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