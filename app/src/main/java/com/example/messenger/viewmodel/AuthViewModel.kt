package com.example.messenger.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.messenger.data.repository.AuthRepository
import com.example.messenger.data.repository.UserRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    data class AuthState(
        val isLoading: Boolean = false,
        val success: Boolean = false,
        val error: String? = null,
        val userId: String? = null
    )

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val userId = firebaseAuth.currentUser?.uid
            if (userId != null) {
                updateUserStatus(userId, "online")
            } else {
                authState.value?.userId?.let { updateUserStatus(it, "offline") }
            }
        }
    }

    fun signIn(email: String, password: String): Task<AuthResult> {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState(error = "Введите email и пароль")
            return auth.signInAnonymously() // Заглушка, чтобы вернуть Task (можно заменить на другой подход)
        }
        _authState.value = AuthState(isLoading = true)
        return authRepository.signIn(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (authRepository.isEmailVerified()) {
                        val userId = authRepository.getCurrentUserId()
                        Log.d("AuthViewModel", "Sign in successful, userId=$userId")
                        _authState.value = AuthState(success = true, userId = userId)
                        updateUserStatus(userId!!, "online")
                    } else {
                        _authState.value = AuthState(error = "Подтвердите email")
                    }
                } else {
                    Log.e("AuthViewModel", "Sign in failed: ${task.exception?.message}")
                    _authState.value = AuthState(error = task.exception?.message)
                }
            }
    }

    fun register(email: String, password: String, repeatPassword: String): Task<AuthResult> {
        if (email.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
            _authState.value = AuthState(error = "Заполните все поля")
            return auth.signInAnonymously() // Заглушка
        }
        if (password != repeatPassword) {
            _authState.value = AuthState(error = "Пароли не совпадают")
            return auth.signInAnonymously() // Заглушка
        }
        _authState.value = AuthState(isLoading = true)
        return authRepository.register(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = authRepository.getCurrentUserId()
                    val userMap = mapOf(
                        "username" to email.substringBefore("@"),
                        "email" to email,
                        "status" to "online"
                    )
                    userRepository.saveUser(userId!!, userMap)
                    Log.d("AuthViewModel", "Register successful, userId=$userId")
                    _authState.value = AuthState(success = true, userId = userId)
                } else {
                    Log.e("AuthViewModel", "Register failed: ${task.exception?.message}")
                    _authState.value = AuthState(error = task.exception?.message)
                }
            }
    }

    fun signOut() {
        val userId = authRepository.getCurrentUserId()
        userId?.let { updateUserStatus(it, "offline") }
        authRepository.signOut()
        _authState.value = AuthState(success = false, userId = null)
        // Очищаем кэш ChatsViewModel при выходе
        val chatsViewModel = ViewModelProvider(ViewModelStoreOwnerProvider.getViewModelStoreOwner()).get(ChatsViewModel::class.java)
        chatsViewModel.clearCache()
    }

    fun getCurrentUserId(): String? {
        val userId = authRepository.getCurrentUserId()
        Log.d("AuthViewModel", "Current userId=$userId")
        return userId
    }

    fun isEmailVerified(): Boolean = authRepository.isEmailVerified()

    fun getFirebaseUser(): FirebaseUser? {
        return auth.currentUser
    }

    private fun updateUserStatus(userId: String, status: String) {
        val userMap = mapOf("status" to status)
        userRepository.saveUser(userId, userMap)
        Log.d("AuthViewModel", "Updated status for userId=$userId to $status")
    }
}

// Временное решение для получения ViewModelStoreOwner
object ViewModelStoreOwnerProvider {
    private lateinit var viewModelStoreOwner: ViewModelStoreOwner

    fun setViewModelStoreOwner(owner: ViewModelStoreOwner) {
        viewModelStoreOwner = owner
    }

    fun getViewModelStoreOwner(): ViewModelStoreOwner {
        return viewModelStoreOwner
    }
}