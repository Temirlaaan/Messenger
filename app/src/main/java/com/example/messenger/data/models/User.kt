package com.example.messenger.data.models

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val status: String = "offline",
    val profileImageUrl: String? = null,

    // Ключи для шифрования
    val publicKey: String? = null,  // Публичный ключ пользователя (хранится в базе данных)
    val privateKey: String? = null  // Приватный ключ (хранится локально, НЕ передается в базу)
) {
    /**
     * Создает копию пользователя без приватного ключа (для передачи в базу данных)
     */
    fun withoutPrivateKey(): User {
        return copy(privateKey = null)
    }

    /**
     * Проверяет, есть ли у пользователя публичный ключ
     */
    fun hasPublicKey(): Boolean {
        return !publicKey.isNullOrEmpty()
    }

    /**
     * Проверяет, есть ли у пользователя приватный ключ (локально)
     */
    fun hasPrivateKey(): Boolean {
        return !privateKey.isNullOrEmpty()
    }

    /**
     * Проверяет, готов ли пользователь к шифрованному общению
     */
    fun isReadyForEncryption(): Boolean {
        return hasPublicKey() && hasPrivateKey()
    }
}