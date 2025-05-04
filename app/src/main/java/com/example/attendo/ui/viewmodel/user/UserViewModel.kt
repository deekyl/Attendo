// app/src/main/java/com/example/attendo/ui/viewmodel/user/UserViewModel.kt
package com.example.attendo.ui.viewmodel.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendo.data.model.user.UserState
import com.example.attendo.data.repositories.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    val userState: StateFlow<UserState> = _userState.asStateFlow()

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
                },
                onFailure = { error ->
                    _userState.value = UserState.Error(error.message ?: "Error desconocido")
                }
            )
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}
