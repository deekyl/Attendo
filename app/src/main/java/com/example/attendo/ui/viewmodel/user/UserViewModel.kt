// app/src/main/java/com/example/attendo/ui/viewmodel/user/UserViewModel.kt
package com.example.attendo.ui.viewmodel.user

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendo.data.dao.interfaces.ProfileImageDao
import com.example.attendo.data.model.user.UserState
import com.example.attendo.data.repositories.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(
    private val authRepository: AuthRepository,
    private val profileImageDao: ProfileImageDao // Añadir el DAO
) : ViewModel() {

    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    // Añadir StateFlow para la imagen de perfil
    private val _profileImageUrl = MutableStateFlow<String?>(null)
    val profileImageUrl: StateFlow<String?> = _profileImageUrl.asStateFlow()

    init {
        loadUserDetails()
    }

    private fun loadUserDetails() {
        viewModelScope.launch {
            _userState.value = UserState.Loading

            authRepository.getCurrentUserDetails().fold(
                onSuccess = { user ->
                    if (!user.isActive) {
                        _userState.value = UserState.Inactive
                    } else if (user.isAdmin) {
                        _userState.value = UserState.Admin(user)
                    } else {
                        _userState.value = UserState.Regular(user)
                    }

                    // Cargar la imagen de perfil después de cargar los datos del usuario
                    loadProfileImage(user.userId)
                },
                onFailure = { error ->
                    _userState.value = UserState.Error(error.message ?: "Error desconocido")
                }
            )
        }
    }

    // Función para cargar la imagen de perfil
    fun loadProfileImage(userId: String) {
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

    // Función para subir una nueva imagen de perfil
    fun uploadProfileImage(userId: String, imageBytes: ByteArray) {
        viewModelScope.launch {
            try {
                val url = profileImageDao.uploadProfileImage(userId, imageBytes)
                if (url != null) {
                    _profileImageUrl.value = url
                    Log.d("Attendo", "Imagen de perfil subida correctamente: $url")
                } else {
                    Log.e("Attendo", "Error: La URL devuelta es nula")
                }
            } catch (e: Exception) {
                Log.e("Attendo", "Error subiendo imagen de perfil: ${e.message}", e)
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}