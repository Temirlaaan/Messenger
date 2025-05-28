package com.example.messenger.data.models

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val status: String = "offline" // Новое поле для статуса
)