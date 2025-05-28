package com.example.messenger.data.models

data class User(
    val uid: String = "",
    val username: String = "", // Основное имя пользователя
    val email: String = ""
    // Убираем name, так как username будет использоваться
)