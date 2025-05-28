package com.example.messenger.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.messenger.data.repository.AuthRepository
import com.example.messenger.data.repository.UserRepository

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    data class AuthState(
        val isLoading: Boolean = false,
        val success: Boolean = false,
        val error: String? = null,
        val userId: String? = null
    )

    fun signIn(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState(error = "Введите email и пароль")
            return
        }
        _authState.value = AuthState(isLoading = true)
        authRepository.signIn(email, password) { success, error ->
            if (success) {
                if (authRepository.isEmailVerified()) {
                    val userId = authRepository.getCurrentUserId()
                    Log.d("AuthViewModel", "Sign in successful, userId=$userId")
                    _authState.value = AuthState(success = true, userId = userId)
                } else {
                    _authState.value = AuthState(error = "Подтвердите email")
                }
            } else {
                Log.e("AuthViewModel", "Sign in failed: $error")
                _authState.value = AuthState(error = error)
            }
        }
    }

    fun register(email: String, password: String, repeatPassword: String) {
        if (email.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
            _authState.value = AuthState(error = "Заполните все поля")
            return
        }
        if (password != repeatPassword) {
            _authState.value = AuthState(error = "Пароли не совпадают")
            return
        }
        _authState.value = AuthState(isLoading = true)
        authRepository.register(email, password) { success, error ->
            if (success) {
                val userId = authRepository.getCurrentUserId()
                val userMap = mapOf(
                    "username" to email.substringBefore("@"),
                    "email" to email
                )
                userRepository.saveUser(userId!!, userMap)
                Log.d("AuthViewModel", "Register successful, userId=$userId")
                _authState.value = AuthState(success = true, userId = userId)
            } else {
                Log.e("AuthViewModel", "Register failed: $error")
                _authState.value = AuthState(error = error)
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _authState.value = AuthState(success = false, userId = null)
    }

    fun getCurrentUserId(): String? {
        val userId = authRepository.getCurrentUserId()
        Log.d("AuthViewModel", "Current userId=$userId")
        return userId
    }

    fun isEmailVerified(): Boolean = authRepository.isEmailVerified()
}