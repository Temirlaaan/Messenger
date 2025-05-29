package com.example.messenger.data.models

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val status: String = "offline",
    val profileImageUrl: String? = null // Изменяем на опциональное поле
)