/*
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

    data class KeyPair(val publicKey: String, val privateKey: String)
    data class EncryptedData(val encryptedContent: String, val iv: String)
    data class EncryptedMessage(
        val encryptedContent: String,
        val encryptedAESKey: String,
        val iv: String
    )

    */
/**
     * Генерация RSA ключей
     *//*

    fun generateRSAKeyPair(): KeyPair {
        return try {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(RSA_KEY_SIZE)
            val keyPair = keyPairGenerator.generateKeyPair()

            val publicKeyString = Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT)
            val privateKeyString = Base64.encodeToString(keyPair.private.encoded, Base64.DEFAULT)

            KeyPair(publicKeyString, privateKeyString)
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error generating RSA key pair", e)
            throw CryptoException("Failed to generate RSA key pair", e)
        }
    }

    */
/**
     * Генерация AES ключа
     *//*

    fun generateAESKey(): SecretKey {
        return try {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(AES_KEY_SIZE)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error generating AES key", e)
            throw CryptoException("Failed to generate AES key", e)
        }
    }

    */
/**
     * Шифрование сообщения с использованием гибридного шифрования (RSA + AES)
     *//*

    fun encryptMessage(message: String, recipientPublicKey: String): EncryptedMessage {
        return try {
            // Генерируем случайный AES ключ для этого сообщения
            val aesKey = generateAESKey()

            // Шифруем сообщение с помощью AES
            val encryptedData = encryptWithAES(message, aesKey)

            // Шифруем AES ключ с помощью RSA публичного ключа получателя
            val encryptedAESKey = encryptAESKeyWithRSA(aesKey, recipientPublicKey)

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

    */
/**
     * Расшифровка сообщения
     *//*

    fun decryptMessage(encryptedMessage: EncryptedMessage, privateKey: String): String {
        return try {
            // Расшифровываем AES ключ с помощью RSA приватного ключа
            val aesKey = decryptAESKeyWithRSA(encryptedMessage.encryptedAESKey, privateKey)

            // Расшифровываем сообщение с помощью AES ключа
            val encryptedData = EncryptedData(encryptedMessage.encryptedContent, encryptedMessage.iv)
            decryptWithAES(encryptedData, aesKey)
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error decrypting message", e)
            throw CryptoException("Failed to decrypt message", e)
        }
    }

    */
/**
     * Шифрование с помощью AES-GCM
     *//*

    private fun encryptWithAES(plaintext: String, secretKey: SecretKey): EncryptedData {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plaintext.toByteArray())

        return EncryptedData(
            encryptedContent = Base64.encodeToString(encryptedBytes, Base64.DEFAULT),
            iv = Base64.encodeToString(iv, Base64.DEFAULT)
        )
    }

    */
/**
     * Расшифровка с помощью AES-GCM
     *//*

    private fun decryptWithAES(encryptedData: EncryptedData, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val iv = Base64.decode(encryptedData.iv, Base64.DEFAULT)
        val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)
        val encryptedBytes = Base64.decode(encryptedData.encryptedContent, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(encryptedBytes)

        return String(decryptedBytes)
    }

    */
/**
     * Шифрование AES ключа с помощью RSA
     *//*

    private fun encryptAESKeyWithRSA(aesKey: SecretKey, publicKeyString: String): String {
        val publicKey = getPublicKeyFromString(publicKeyString)
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        val encryptedKey = cipher.doFinal(aesKey.encoded)
        return Base64.encodeToString(encryptedKey, Base64.DEFAULT)
    }

    */
/**
     * Расшифровка AES ключа с помощью RSA
     *//*

    private fun decryptAESKeyWithRSA(encryptedAESKey: String, privateKeyString: String): SecretKey {
        val privateKey = getPrivateKeyFromString(privateKeyString)
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)

        val encryptedKeyBytes = Base64.decode(encryptedAESKey, Base64.DEFAULT)
        val decryptedKeyBytes = cipher.doFinal(encryptedKeyBytes)

        return SecretKeySpec(decryptedKeyBytes, "AES")
    }

    */
/**
     * Получение публичного ключа из строки
     *//*

    private fun getPublicKeyFromString(publicKeyString: String): PublicKey {
        val keyBytes = Base64.decode(publicKeyString, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }

    */
/**
     * Получение приватного ключа из строки
     *//*

    private fun getPrivateKeyFromString(privateKeyString: String): PrivateKey {
        val keyBytes = Base64.decode(privateKeyString, Base64.DEFAULT)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(keySpec)
    }

    */
/**
     * Создание хеша для верификации целостности сообщения
     *//*

    fun createMessageHash(message: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(message.toByteArray())
            Base64.encodeToString(hashBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error creating message hash", e)
            throw CryptoException("Failed to create message hash", e)
        }
    }

    */
/**
     * Проверка хеша сообщения
     *//*

    fun verifyMessageHash(message: String, expectedHash: String): Boolean {
        return try {
            val actualHash = createMessageHash(message)
            actualHash == expectedHash
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error verifying message hash", e)
            false
        }
    }
}

class CryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)*/
