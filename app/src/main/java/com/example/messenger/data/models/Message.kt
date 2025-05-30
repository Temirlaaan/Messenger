package com.example.messenger.data.models

import java.util.Date

data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
    val type: String = "text", // "text" или "image"
    val imageUrl: String? = null, // URL изображения, если type == "image"
    val isEncrypted: Boolean = false, // Флаг, указывающий, зашифровано ли сообщение
    val encryptedAESKey: String? = null, // Зашифрованный AES ключ (для получателя)
    val iv: String? = null, // Вектор инициализации для AES
    val messageHash: String? = null, // Хеш для проверки целостности
    val timeSlot: Long = 0L // Временной отрезок (например, номер часа)
) {
    fun getTimestampAsDate(): Date = Date(timestamp)

    /**
     * Проверяет, является ли сообщение зашифрованным и содержит ли все необходимые данные
     */
    fun isValidEncryptedMessage(): Boolean {
        return isEncrypted && !encryptedAESKey.isNullOrEmpty() && !iv.isNullOrEmpty()
    }

    /**
     * Создает копию сообщения с данными шифрования
     */
    fun withEncryption(
        encryptedContent: String,
        encryptedAESKey: String,
        iv: String,
        messageHash: String? = null,
        timeSlot: Long
    ): Message {
        return copy(
            content = encryptedContent,
            isEncrypted = true,
            encryptedAESKey = encryptedAESKey,
            iv = iv,
            messageHash = messageHash,
            timeSlot = timeSlot
        )
    }

    /**
     * Создает копию сообщения с расшифрованным содержимым
     */
    fun withDecryptedContent(decryptedContent: String): Message {
        return copy(
            content = decryptedContent,
            isEncrypted = false,
            encryptedAESKey = null,
            iv = null,
            messageHash = null
        )
    }
}