package com.example.attendo.ui.viewmodel.user

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendo.data.dao.interfaces.ProfileImageDao
import com.example.attendo.data.dao.interfaces.UserDao
import com.example.attendo.data.model.user.ProfileEditData
import com.example.attendo.data.model.user.ProfileEditState
import com.example.attendo.data.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userDao: UserDao,
    private val profileImageDao: ProfileImageDao
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _profileImageUrl = MutableStateFlow<String?>(null)
    val profileImageUrl: StateFlow<String?> = _profileImageUrl.asStateFlow()

    private val _editState = MutableStateFlow<ProfileEditState>(ProfileEditState.Idle)
    val editState: StateFlow<ProfileEditState> = _editState.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    // Estados para edición
    private val _editData = MutableStateFlow(ProfileEditData())
    val editData: StateFlow<ProfileEditData> = _editData.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    fun loadUser(userId: String) {
        viewModelScope.launch {
            try {
                _editState.value = ProfileEditState.Loading

                val userData = userDao.getUserById(userId)
                if (userData != null) {
                    _user.value = userData

                    // Cargar datos de edición
                    _editData.value = ProfileEditData(
                        fullName = userData.fullName,
                        email = userData.email,
                        documentId = userData.documentId,
                        address = userData.address
                    )

                    // Cargar imagen de perfil
                    loadProfileImage(userId)

                    _editState.value = ProfileEditState.Idle
                } else {
                    _editState.value = ProfileEditState.Error("Usuario no encontrado")
                }
            } catch (e: Exception) {
                Log.e("Attendo", "Error cargando usuario: ${e.message}", e)
                _editState.value = ProfileEditState.Error("Error al cargar el perfil")
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
                    _editState.value = ProfileEditState.Success
                } else {
                    _editState.value = ProfileEditState.Error("Error al subir la imagen")
                }
            } catch (e: Exception) {
                Log.e("Attendo", "Error subiendo imagen: ${e.message}", e)
                _editState.value = ProfileEditState.Error("Error al subir la imagen: ${e.message}")
            } finally {
                _isUploadingImage.value = false
            }
        }
    }

    fun updateEditData(newData: ProfileEditData) {
        _editData.value = newData
    }

    fun enableEditMode() {
        _isEditMode.value = true
        // Recargar datos actuales
        _user.value?.let { currentUser ->
            _editData.value = ProfileEditData(
                fullName = currentUser.fullName,
                email = currentUser.email,
                documentId = currentUser.documentId,
                address = currentUser.address
            )
        }
    }

    fun cancelEdit() {
        _isEditMode.value = false
        // Restaurar datos originales
        _user.value?.let { currentUser ->
            _editData.value = ProfileEditData(
                fullName = currentUser.fullName,
                email = currentUser.email,
                documentId = currentUser.documentId,
                address = currentUser.address
            )
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            try {
                _editState.value = ProfileEditState.Loading

                val currentUser = _user.value
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(
                        fullName = _editData.value.fullName,
                        email = _editData.value.email,
                        documentId = _editData.value.documentId,
                        address = _editData.value.address
                    )

                    val success = userDao.updateUser(updatedUser)
                    if (success) {
                        _user.value = updatedUser
                        _isEditMode.value = false
                        _editState.value = ProfileEditState.Success
                    } else {
                        _editState.value = ProfileEditState.Error("Error al actualizar el perfil")
                    }
                } else {
                    _editState.value = ProfileEditState.Error("Usuario no encontrado")
                }
            } catch (e: Exception) {
                Log.e("Attendo", "Error guardando perfil: ${e.message}", e)
                _editState.value = ProfileEditState.Error("Error al guardar: ${e.message}")
            }
        }
    }

    fun clearState() {
        _editState.value = ProfileEditState.Idle
    }

    fun validateForm(): String? {
        val data = _editData.value
        return when {
            data.fullName.isBlank() -> "El nombre no puede estar vacío"
            data.email.isBlank() -> "El email no puede estar vacío"
            !isValidEmail(data.email) -> "El formato del email no es válido"
            data.documentId.isBlank() -> "El documento no puede estar vacío"
            data.address.isBlank() -> "La dirección no puede estar vacía"
            else -> null
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }
}