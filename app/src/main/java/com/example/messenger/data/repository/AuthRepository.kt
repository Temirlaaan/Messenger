package com.example.messenger.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    fun signIn(email: String, password: String): Task<AuthResult> {
        Log.d("AuthRepository", "Attempting to sign in with email: $email")
        return auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d("AuthRepository", "Sign in successful")
            }
            .addOnFailureListener { e ->
                Log.e("AuthRepository", "Sign in failed: ${e.message}")
            }
    }

    fun register(email: String, password: String): Task<AuthResult> {
        Log.d("AuthRepository", "Attempting to register with email: $email")
        return auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                Log.d("AuthRepository", "Register successful")
                // Отправляем письмо верификации
                authResult.user?.sendEmailVerification()
                    ?.addOnSuccessListener {
                        Log.d("AuthRepository", "Verification email sent")
                    }
                    ?.addOnFailureListener { e ->
                        Log.e("AuthRepository", "Failed to send verification email: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("AuthRepository", "Register failed: ${e.message}")
            }
    }

    fun signOut() {
        Log.d("AuthRepository", "Signing out")
        auth.signOut()
    }

    fun getCurrentUserId(): String? {
        val userId = auth.currentUser?.uid
        Log.d("AuthRepository", "Current userId: $userId")
        return userId
    }

    fun isEmailVerified(): Boolean {
        val isVerified = auth.currentUser?.isEmailVerified ?: false
        Log.d("AuthRepository", "Email verified: $isVerified")
        return isVerified
    }

    fun sendEmailVerification(): Task<Void>? {
        return auth.currentUser?.sendEmailVerification()
    }

    fun reloadUser(): Task<Void>? {
        return auth.currentUser?.reload()
    }
}