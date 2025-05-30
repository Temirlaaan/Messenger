package com.example.messenger.utils

import android.util.Base64
import android.util.Log
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val RSA_KEY_SIZE = 2048
    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 16
    private const val TIME_SLOT_DURATION = 3600_000L // 1 час в миллисекундах

    data class KeyPair(val publicKey: String, val privateKey: String)
    data class EncryptedData(val encryptedContent: String, val iv: String)
    data class EncryptedMessage(
        val encryptedContent: String,
        val encryptedAESKey: String,
        val iv: String
    )

    /**
     * Генерация RSA ключей для нового пользователя
     */
    fun generateRSAKeyPair(): KeyPair {
        return try {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(RSA_KEY_SIZE)
            val keyPair = keyPairGenerator.generateKeyPair()

            val publicKeyString = Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT)
            val privateKeyString = Base64.encodeToString(keyPair.private.encoded, Base64.DEFAULT)

            Log.d("CryptoManager", "RSA key pair generated successfully")
            KeyPair(publicKeyString, privateKeyString)
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error generating RSA key pair", e)
            throw CryptoException("Failed to generate RSA key pair", e)
        }
    }

    /**
     * Генерация AES ключа для шифрования сообщения на основе временного отрезка
     */
    private fun generateTimeBasedAESKey(timeSlot: Long): SecretKey {
        return try {
            val seed = timeSlot.toString().toByteArray()
            val secureRandom = SecureRandom(seed)
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(AES_KEY_SIZE, secureRandom)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error generating time-based AES key", e)
            throw CryptoException("Failed to generate AES key", e)
        }
    }

    /**
     * Шифрование сообщения с использованием гибридного шифрования (RSA + AES)
     * @param message Текст сообщения для шифрования
     * @param recipientPublicKey Публичный ключ получателя
     * @param timestamp Время отправки для определения временного отрезка
     * @return Зашифрованное сообщение
     */
    fun encryptMessage(message: String, recipientPublicKey: String, timestamp: Long): EncryptedMessage {
        return try {
            Log.d("CryptoManager", "Encrypting message of length: ${message.length}")

            // Определяем временной отрезок
            val timeSlot = timestamp / TIME_SLOT_DURATION
            val aesKey = generateTimeBasedAESKey(timeSlot)

            // Шифруем сообщение с помощью AES
            val encryptedData = encryptWithAES(message, aesKey)

            // Шифруем AES ключ с помощью RSA публичного ключа получателя
            val encryptedAESKey = encryptAESKeyWithRSA(aesKey, recipientPublicKey)

            Log.d("CryptoManager", "Message encrypted successfully")
            EncryptedMessage(
                encryptedContent = encryptedData.encryptedContent,
                encryptedAESKey = encryptedAESKey,
                iv = encryptedData.iv
            )
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error encrypting message", e)
            throw CryptoException("Failed to encrypt message", e)
        }
    }

    /**
     * Расшифровка сообщения
     * @param encryptedMessage Зашифрованное сообщение
     * @param privateKey Приватный ключ получателя
     * @param timeSlot Временной отрезок для генерации ключа
     * @return Расшифрованный текст сообщения
     */
    fun decryptMessage(encryptedMessage: EncryptedMessage, privateKey: String, timeSlot: Long): String {
        return try {
            Log.d("CryptoManager", "Decrypting message")

            // Генерируем AES ключ на основе того же временного отрезка
            val aesKey = generateTimeBasedAESKey(timeSlot)

            // Расшифровываем сообщение с помощью AES ключа
            val encryptedData = EncryptedData(encryptedMessage.encryptedContent, encryptedMessage.iv)
            val decryptedMessage = decryptWithAES(encryptedData, aesKey)

            Log.d("CryptoManager", "Message decrypted successfully")
            decryptedMessage
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error decrypting message", e)
            throw CryptoException("Failed to decrypt message", e)
        }
    }

    /**
     * Шифрование с помощью AES-GCM
     */
    private fun encryptWithAES(plaintext: String, secretKey: SecretKey): EncryptedData {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        return EncryptedData(
            encryptedContent = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    /**
     * Расшифровка с помощью AES-GCM
     */
    private fun decryptWithAES(encryptedData: EncryptedData, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val iv = Base64.decode(encryptedData.iv, Base64.NO_WRAP)
        val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)
        val encryptedBytes = Base64.decode(encryptedData.encryptedContent, Base64.NO_WRAP)
        val decryptedBytes = cipher.doFinal(encryptedBytes)

        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Шифрование AES ключа с помощью RSA
     */
    private fun encryptAESKeyWithRSA(aesKey: SecretKey, publicKeyString: String): String {
        val publicKey = getPublicKeyFromString(publicKeyString)
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        val encryptedKey = cipher.doFinal(aesKey.encoded)
        return Base64.encodeToString(encryptedKey, Base64.NO_WRAP)
    }

    /**
     * Расшифровка AES ключа с помощью RSA
     */
    private fun decryptAESKeyWithRSA(encryptedAESKey: String, privateKeyString: String): SecretKey {
        val privateKey = getPrivateKeyFromString(privateKeyString)
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)

        val encryptedKeyBytes = Base64.decode(encryptedAESKey, Base64.NO_WRAP)
        val decryptedKeyBytes = cipher.doFinal(encryptedKeyBytes)

        return SecretKeySpec(decryptedKeyBytes, "AES")
    }

    /**
     * Получение публичного ключа из строки
     */
    private fun getPublicKeyFromString(publicKeyString: String): PublicKey {
        val keyBytes = Base64.decode(publicKeyString.trim(), Base64.NO_WRAP)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }

    /**
     * Получение приватного ключа из строки
     */
    private fun getPrivateKeyFromString(privateKeyString: String): PrivateKey {
        val keyBytes = Base64.decode(privateKeyString.trim(), Base64.NO_WRAP)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(keySpec)
    }

    /**
     * Создание хеша для верификации целостности сообщения
     */
    fun createMessageHash(message: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(message.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(hashBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error creating message hash", e)
            throw CryptoException("Failed to create message hash", e)
        }
    }

    /**
     * Проверка хеша сообщения
     */
    fun verifyMessageHash(message: String, expectedHash: String): Boolean {
        return try {
            val actualHash = createMessageHash(message)
            actualHash == expectedHash
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error verifying message hash", e)
            false
        }
    }

    /**
     * Проверка валидности публичного ключа
     */
    fun isValidPublicKey(publicKeyString: String): Boolean {
        return try {
            getPublicKeyFromString(publicKeyString)
            true
        } catch (e: Exception) {
            Log.w("CryptoManager", "Invalid public key", e)
            false
        }
    }

    /**
     * Проверка валидности приватного ключа
     */
    fun isValidPrivateKey(privateKeyString: String): Boolean {
        return try {
            getPrivateKeyFromString(privateKeyString)
            true
        } catch (e: Exception) {
            Log.w("CryptoManager", "Invalid private key", e)
            false
        }
    }
}

class CryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)