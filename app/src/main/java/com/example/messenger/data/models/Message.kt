package com.example.messenger.data.models

import java.util.Date

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0L, // Изменяем на Long
    val isRead: Boolean = false
) {
    // Метод для получения Date из timestamp
    fun getTimestampAsDate(): Date = Date(timestamp)
}