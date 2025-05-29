package com.example.messenger.data.models

import java.util.Date

data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
    val type: String = "text", // "text" или "image"
    val imageUrl: String? = null // URL изображения, если type == "image"
) {
    fun getTimestampAsDate(): Date = Date(timestamp)
}