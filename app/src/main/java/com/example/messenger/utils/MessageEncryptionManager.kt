package com.example.messenger.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.messenger.data.models.Message
import com.example.messenger.data.models.User

class MessageEncryptionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "encryption_prefs"
        private const val PRIVATE_KEY_PREFIX = "private_key_"
        private const val PUBLIC_KEY_PREFIX = "public_key_"
        private const val ENCRYPTION_ENABLED = "encryption_enabled"
        private const val TIME_SLOT_DURATION = 3600_000L // 1 час в миллисекундах
    }

    /**
     * Генерирует ключи для пользователя и сохраняет приватный ключ локально
     */
    fun generateKeysForUser(userId: String): CryptoManager.KeyPair {
        return try {
            Log.d("MessageEncryptionManager", "Generating keys for user: $userId")
            val keyPair = CryptoManager.generateRSAKeyPair()

            // Сохраняем приватный ключ локально
            savePrivateKey(userId, keyPair.privateKey)

            Log.d("MessageEncryptionManager", "Keys generated and private key saved for user: $userId")
            keyPair
        } catch (e: Exception) {
            Log.e("MessageEncryptionManager", "Error generating keys for user: $userId", e)
            throw e
        }
    }

    /**
     * Сохраняет приватный ключ пользователя локально
     */
    fun savePrivateKey(userId: String, privateKey: String) {
        prefs.edit().putString(PRIVATE_KEY_PREFIX + userId, privateKey).apply()
        Log.d("MessageEncryptionManager", "Private key saved for user: $userId")
    }

    /**
     * Получает приватный ключ пользователя
     */
    fun getPrivateKey(userId: String): String? {
        return prefs.getString(PRIVATE_KEY_PREFIX + userId, null)
    }

    /**
     * Сохраняет публичный ключ пользователя локально (для кэширования)
     */
    fun savePublicKey(userId: String, publicKey: String) {
        prefs.edit().putString(PUBLIC_KEY_PREFIX + userId, publicKey).apply()
    }

    /**
     * Получает кэшированный публичный ключ пользователя
     */
    fun getCachedPublicKey(userId: String): String? {
        return prefs.getString(PUBLIC_KEY_PREFIX + userId, null)
    }

    /**
     * Проверяет, включено ли шифрование
     */
    fun isEncryptionEnabled(): Boolean {
        return prefs.getBoolean(ENCRYPTION_ENABLED, true) // По умолчанию включено
    }

    /**
     * Устанавливает состояние шифрования
     */
    fun setEncryptionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(ENCRYPTION_ENABLED, enabled).apply()
        Log.d("MessageEncryptionManager", "Encryption enabled: $enabled")
    }

    /**
     * Шифрует сообщение для отправки
     */
    fun encryptMessage(message: Message, recipientPublicKey: String): Message {
        return try {
            if (!isEncryptionEnabled()) {
                Log.d("MessageEncryptionManager", "Encryption disabled, sending plain message")
                return message
            }

            Log.d("MessageEncryptionManager", "Encrypting message from ${message.senderId} to ${message.receiverId}")

            val timeSlot = message.timestamp / TIME_SLOT_DURATION
            val encryptedData = CryptoManager.encryptMessage(message.content, recipientPublicKey, message.timestamp)
            val messageHash = CryptoManager.createMessageHash(message.content)

            val encryptedMessage = message.withEncryption(
                encryptedContent = encryptedData.encryptedContent,
                encryptedAESKey = encryptedData.encryptedAESKey,
                iv = encryptedData.iv,
                messageHash = messageHash,
                timeSlot = timeSlot
            )

            Log.d("MessageEncryptionManager", "Message encrypted successfully")
            encryptedMessage
        } catch (e: Exception) {
            Log.e("MessageEncryptionManager", "Error encrypting message", e)
            message.copy(isEncrypted = false)
        }
    }

    /**
     * Расшифровывает полученное сообщение
     */
    fun decryptMessage(message: Message, privateKey: String): Message {
        return try {
            if (!message.isEncrypted || !message.isValidEncryptedMessage()) {
                Log.d("MessageEncryptionManager", "Message is not encrypted, returning as is")
                return message
            }

            Log.d("MessageEncryptionManager", "Decrypting message from ${message.senderId} to ${message.receiverId}")

            val encryptedData = CryptoManager.EncryptedMessage(
                encryptedContent = message.content,
                encryptedAESKey = message.encryptedAESKey!!,
                iv = message.iv!!
            )

            val decryptedContent = CryptoManager.decryptMessage(encryptedData, privateKey, message.timeSlot)

            message.messageHash?.let { hash ->
                if (!CryptoManager.verifyMessageHash(decryptedContent, hash)) {
                    Log.w("MessageEncryptionManager", "Message hash verification failed")
                }
            }

            val decryptedMessage = message.withDecryptedContent(decryptedContent)

            Log.d("MessageEncryptionManager", "Message decrypted successfully")
            decryptedMessage
        } catch (e: Exception) {
            Log.e("MessageEncryptionManager", "Error decrypting message", e)
            message.copy(content = "[Ошибка расшифровки сообщения]", isEncrypted = false)
        }
    }

    /**
     * Расшифровывает список сообщений
     */
    fun decryptMessages(messages: List<Message>, privateKey: String): List<Message> {
        return messages.map { message ->
            decryptMessage(message, privateKey)
        }
    }

    /**
     * Проверяет, есть ли у пользователя ключи для шифрования
     */
    fun hasKeysForUser(userId: String): Boolean {
        return getPrivateKey(userId) != null
    }

    /**
     * Удаляет все ключи пользователя (при выходе из аккаунта)
     */
    fun clearKeysForUser(userId: String) {
        prefs.edit()
            .remove(PRIVATE_KEY_PREFIX + userId)
            .remove(PUBLIC_KEY_PREFIX + userId)
            .apply()
        Log.d("MessageEncryptionManager", "Keys cleared for user: $userId")
    }

    /**
     * Удаляет все сохраненные ключи
     */
    fun clearAllKeys() {
        val editor = prefs.edit()
        val allKeys = prefs.all.keys

        allKeys.forEach { key ->
            if (key.startsWith(PRIVATE_KEY_PREFIX) || key.startsWith(PUBLIC_KEY_PREFIX)) {
                editor.remove(key)
            }
        }

        editor.apply()
        Log.d("MessageEncryptionManager", "All keys cleared")
    }
}