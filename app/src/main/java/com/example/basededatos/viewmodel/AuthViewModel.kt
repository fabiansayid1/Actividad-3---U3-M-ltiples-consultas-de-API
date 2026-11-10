package com.example.basededatos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basededatos.network.UserProfile
import com.example.basededatos.network.UserStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val userStore: UserStore
) : ViewModel() {

    private val _state = MutableLiveData(AuthUiState())
    val state: LiveData<AuthUiState> = _state

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            try {
                val profile = userStore.login(email, password)
                _state.value = if (profile == null) {
                    AuthUiState(errorMessage = "Correo o contraseña incorrectos.")
                } else {
                    AuthUiState(profile = profile)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = AuthUiState(errorMessage = "No se pudo iniciar sesión. Inténtalo de nuevo.")
            }
        }
    }

    fun register(profile: UserProfile, password: String) {
        viewModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            try {
                val saved = userStore.register(profile, password)
                _state.value = if (saved) {
                    AuthUiState(profile = profile)
                } else {
                    AuthUiState(errorMessage = "Ese correo ya está registrado.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = AuthUiState(errorMessage = "No se pudo crear la cuenta. Inténtalo de nuevo.")
            }
        }
    }
}
