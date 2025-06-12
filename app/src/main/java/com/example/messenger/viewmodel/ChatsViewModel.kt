package com.example.messenger.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.messenger.data.models.Message
import com.example.messenger.data.models.User
import com.example.messenger.data.repository.ChatRepository
import com.example.messenger.data.repository.UserRepository
import com.example.messenger.utils.MessageEncryptionManager
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject

class ChatsViewModel(
    private val authViewModel: AuthViewModel,
    private val context: android.content.Context
) : ViewModel() {

    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()
    private val storage = FirebaseStorage.getInstance()
    private val encryptionManager = MessageEncryptionManager(context)
    private val client = OkHttpClient()

    private val _chatsState = MutableLiveData<ChatsState>()
    val chatsState: LiveData<ChatsState> = _chatsState

    private val _messagesState = MutableLiveData<MessagesState>()
    val messagesState: LiveData<MessagesState> = _messagesState

    private val _translationState = MutableLiveData<TranslationState>()
    val translationState: LiveData<TranslationState> = _translationState

    private var cachedChats: List<Triple<String, Message?, Int>>? = null
    private var cachedUsers: List<User>? = null
    private var cachedMessages: List<Message>? = null
    private var isSendingInProgress = false

    // Добавляем Set для отслеживания отправляемых сообщений
    private val pendingMessages = mutableSetOf<String>()

    data class ChatsState(
        val chats: List<Triple<String, Message?, Int>> = emptyList(),
        val users: List<User> = emptyList(),
        val error: String? = null,
        val isLoading: Boolean = false
    )

    data class MessagesState(
        val messages: List<Message> = emptyList(),
        val error: String? = null,
        val isLoading: Boolean = false,
        val sendSuccess: Boolean = false,
        val isSending: Boolean = false
    )

    data class TranslationState(
        val isTranslating: Boolean = false,
        val translatedMessagePosition: Int = -1,
        val error: String? = null
    )

    fun loadChats(userId: String) {
        if (cachedChats != null && cachedUsers != null) {
            Log.d("ChatsViewModel", "Returning cached chats: ${cachedChats!!.size}, users: ${cachedUsers!!.size}")
            _chatsState.value = ChatsState(
                chats = cachedChats!!,
                users = cachedUsers!!,
                isLoading = false
            )
            return
        }

        Log.d("ChatsViewModel", "Loading chats for userId=$userId")
        _chatsState.value = ChatsState(isLoading = true)
        chatRepository.getChatsWithLastMessage(userId, useSingleEvent = cachedChats != null) { chatPairs ->
            if (chatPairs.isEmpty()) {
                Log.d("ChatsViewModel", "No chats found for userId=$userId")
                cachedChats = emptyList()
                _chatsState.value = ChatsState(chats = emptyList(), isLoading = false)
                return@getChatsWithLastMessage
            }
            val chatTriples = mutableListOf<Triple<String, Message?, Int>>()
            chatPairs.forEach { (chatId, lastMessage) ->
                chatRepository.getUnreadCount(userId, chatId) { unreadCount ->
                    chatTriples.add(Triple(chatId, lastMessage?.let { decryptMessageIfNeeded(it) }, unreadCount))
                    if (chatTriples.size == chatPairs.size) {
                        Log.d("ChatsViewModel", "Chats loaded: ${chatTriples.size}")
                        cachedChats = chatTriples
                        _chatsState.value = ChatsState(chats = chatTriples, isLoading = false)
                        val userIds = chatPairs.map { it.first }.distinct()
                        userRepository.getUsersByIds(userIds) { users ->
                            Log.d("ChatsViewModel", "Users loaded for chats: ${users.size}")
                            cachedUsers = users
                            _chatsState.value = _chatsState.value?.copy(users = users, isLoading = false)
                        }
                    }
                }
            }
        }
    }

    fun loadMessages(senderId: String, receiverId: String) {
        Log.d("ChatsViewModel", "Loading messages between senderId=$senderId and receiverId=$receiverId")
        _messagesState.value = MessagesState(isLoading = true)
        chatRepository.getMessages(senderId, receiverId) { messages ->
            val privateKey = encryptionManager.getPrivateKey(senderId)
            if (privateKey != null) {
                val decryptedMessages = encryptionManager.decryptMessages(messages, privateKey)
                    .sortedBy { it.timestamp }

                // Объединяем новые сообщения с уникальными идентификаторами
                val uniqueMessages = mergeUniqueMessages(decryptedMessages)

                if (cachedMessages != uniqueMessages) {
                    cachedMessages = uniqueMessages
                    _messagesState.value = MessagesState(messages = uniqueMessages, isLoading = false)
                } else {
                    _messagesState.value = MessagesState(messages = cachedMessages!!, isLoading = false)
                }
            } else {
                Log.w("ChatsViewModel", "No private key found for user $senderId, messages not decrypted")
                val sortedMessages = messages.sortedBy { it.timestamp }
                val uniqueMessages = mergeUniqueMessages(sortedMessages)
                cachedMessages = uniqueMessages
                _messagesState.value = MessagesState(messages = uniqueMessages, isLoading = false)
            }
        }
    }

    fun sendMessage(message: Message) {
        Log.d("ChatsViewModel", "Sending message: ${message.content}, isSendingInProgress=$isSendingInProgress")
        if (isSendingInProgress) return

        // Создаем уникальный ключ для сообщения
        val messageKey = "${message.senderId}_${message.receiverId}_${message.timestamp}_${message.content.hashCode()}"

        // Проверяем, не отправляется ли уже это сообщение
        if (pendingMessages.contains(messageKey)) {
            Log.d("ChatsViewModel", "Message already being sent, skipping")
            return
        }

        pendingMessages.add(messageKey)

        val currentState = _messagesState.value ?: MessagesState()
        isSendingInProgress = true
        _messagesState.value = currentState.copy(isSending = true)

        val receiverId = if (message.senderId == authViewModel.getCurrentUserId()) message.receiverId else message.senderId
        userRepository.getUser(receiverId) { receiver ->
            if (receiver.hasPublicKey()) {
                val publicKey = receiver.publicKey!!
                val encryptedMessage = encryptionManager.encryptMessage(message, publicKey)
                chatRepository.sendMessage(encryptedMessage) { success, error ->
                    handleSendResult(success, error, encryptedMessage, messageKey)
                }
            } else {
                Log.w("ChatsViewModel", "Receiver $receiverId has no public key, sending unencrypted")
                val unencryptedMessage = message.copy(isEncrypted = false)
                chatRepository.sendMessage(unencryptedMessage) { success, error ->
                    handleSendResult(success, error, unencryptedMessage, messageKey)
                }
            }
        }
    }

    fun translateMessage(message: Message, position: Int) {
        if (message.translatedContent != null && message.translatedContent.isNotEmpty()) {
            val updatedMessage = message.copy(translatedContent = null)
            updateMessageInList(updatedMessage, position)
            return
        }

        if (message.type != "text" || message.content.isBlank()) {
            Log.w("ChatsViewModel", "Skipping translation: type=${message.type}, content='${message.content}'")
            return
        }

        val needsTranslation = needsTranslation(message.content)
        if (!needsTranslation) {
            Log.d("ChatsViewModel", "Message doesn't need translation: ${message.content}")
            return
        }

        _translationState.value = TranslationState(isTranslating = true, translatedMessagePosition = position)

        val targetLanguage = if (isRussian(message.content)) "en" else "ru"
        Log.d("ChatsViewModel", "Attempting to translate: content='${message.content}', target=$targetLanguage")

        val requestUrl = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLanguage&dt=t&q=${java.net.URLEncoder.encode(message.content, "UTF-8")}"
        val request = Request.Builder()
            .url(requestUrl)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                Log.e("ChatsViewModel", "Translation failed: ${e.message}")
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    _translationState.value = TranslationState(isTranslating = false, error = "Ошибка перевода")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                    Log.e("ChatsViewModel", "Unsuccessful response: code=${response.code}, body=$responseBody")
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        _translationState.value = TranslationState(isTranslating = false, error = "Ошибка сервера перевода")
                    }
                    return
                }

                try {
                    val jsonArray = JSONArray(responseBody)
                    if (jsonArray.length() > 0) {
                        val translationArray = jsonArray.getJSONArray(0)
                        if (translationArray.length() > 0) {
                            val translatedText = translationArray.getJSONArray(0).getString(0)
                            if (translatedText.isNotEmpty() && translatedText.lowercase() != message.content.lowercase()) {
                                Log.d("ChatsViewModel", "Translation successful: '$translatedText'")
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    val updatedMessage = message.copy(translatedContent = translatedText)
                                    updateMessageInList(updatedMessage, position)
                                    _translationState.value = TranslationState(isTranslating = false)
                                }
                            } else {
                                Log.d("ChatsViewModel", "Translation same as original or empty")
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    _translationState.value = TranslationState(isTranslating = false, error = "Перевод не требуется")
                                }
                            }
                        }
                    } else {
                        Log.e("ChatsViewModel", "Could not parse translation response: $responseBody")
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            _translationState.value = TranslationState(isTranslating = false, error = "Ошибка обработки ответа")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatsViewModel", "Error parsing response: ${e.message}")
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        _translationState.value = TranslationState(isTranslating = false, error = "Ошибка обработки ответа")
                    }
                }
            }
        })
    }

    fun uploadAndSendImage(senderId: String, receiverId: String, imageUri: Uri, callback: (Boolean, String?) -> Unit) {
        Log.d("ChatsViewModel", "Uploading image for senderId=$senderId, receiverId=$receiverId")

        val currentState = _messagesState.value ?: MessagesState()
        _messagesState.value = currentState.copy(isSending = true)

        val storageRef = storage.reference
        val imageRef = storageRef.child("chat_images/${UUID.randomUUID()}.jpg")
        val uploadTask = imageRef.putFile(imageUri)

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUrl = task.result.toString()
                val message = Message(
                    senderId = senderId,
                    receiverId = receiverId,
                    content = "Image",
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    type = "image",
                    imageUrl = downloadUrl
                )
                sendMessage(message)
                callback(true, null)
            } else {
                Log.e("ChatsViewModel", "Error uploading image: ${task.exception?.message}")
                _messagesState.value = currentState.copy(isSending = false, error = task.exception?.message)
                callback(false, task.exception?.message)
            }
        }
    }

    fun markAsRead(senderId: String, receiverId: String) {
        val currentUserId = authViewModel.getCurrentUserId() ?: return
        Log.d("ChatsViewModel", "Marking messages as read for senderId=$senderId, receiverId=$receiverId")
        chatRepository.markAsRead(senderId, receiverId, currentUserId) { success, error ->
            if (success) {
                Log.d("ChatsViewModel", "Messages marked as read")
                loadChats(currentUserId)
            } else {
                Log.e("ChatsViewModel", "Error marking as read: $error")
                _chatsState.value = _chatsState.value?.copy(error = error, isLoading = false)
            }
        }
    }

    fun clearCache() {
        cachedChats = null
        cachedUsers = null
        cachedMessages = null
        isSendingInProgress = false
        pendingMessages.clear() // Очищаем pending сообщения
        Log.d("ChatsViewModel", "Cache cleared")
    }

    // ИСПРАВЛЕННЫЙ метод handleSendResult - НЕ добавляем сообщение локально
    private fun handleSendResult(success: Boolean, error: String?, message: Message, messageKey: String) {
        // Удаляем сообщение из pending
        pendingMessages.remove(messageKey)

        if (success) {
            Log.d("ChatsViewModel", "Message sent successfully")
            val currentState = _messagesState.value ?: MessagesState()

            // НЕ добавляем сообщение локально - оно придет через Firebase listener
            _messagesState.value = currentState.copy(
                isSending = false,
                sendSuccess = true
                // НЕ обновляем messages здесь!
            )

            isSendingInProgress = false
        } else {
            Log.e("ChatsViewModel", "Error sending message: $error")
            _messagesState.value = MessagesState(
                error = error,
                isLoading = false,
                isSending = false
            )
            isSendingInProgress = false
        }
    }

    // Новый метод для объединения уникальных сообщений
    private fun mergeUniqueMessages(newMessages: List<Message>): List<Message> {
        val currentMessages = cachedMessages ?: emptyList()
        val allMessages = (currentMessages + newMessages).distinctBy { message ->
            // Создаем уникальный ключ на основе содержимого сообщения
            "${message.senderId}_${message.receiverId}_${message.timestamp}_${message.content}_${message.type}"
        }.sortedBy { it.timestamp }

        return allMessages
    }

    private fun updateMessageInList(updatedMessage: Message, position: Int) {
        val currentMessages = cachedMessages?.toMutableList() ?: return
        if (position >= 0 && position < currentMessages.size) {
            currentMessages[position] = updatedMessage
            cachedMessages = currentMessages
            _messagesState.value = _messagesState.value?.copy(messages = currentMessages)
        }
    }

    private fun decryptMessageIfNeeded(message: Message): Message {
        val userId = authViewModel.getCurrentUserId() ?: return message
        val privateKey = encryptionManager.getPrivateKey(userId) ?: return message
        return encryptionManager.decryptMessage(message, privateKey)
    }

    private fun isRussian(text: String): Boolean {
        return text.any { it in 'а'..'я' || it in 'А'..'Я' || it in 'ё'..'ё' || it in 'Ё'..'Ё' }
    }

    private fun isEnglish(text: String): Boolean {
        return text.any { it in 'a'..'z' || it in 'A'..'Z' }
    }

    private fun needsTranslation(text: String): Boolean {
        val cleanText = text.trim()
        if (cleanText.length < 2) return false
        if (cleanText.all { !it.isLetter() }) return false
        val hasRussian = isRussian(cleanText)
        val hasEnglish = isEnglish(cleanText)
        if (hasRussian && hasEnglish) {
            Log.d("ChatsViewModel", "Mixed language text, skipping translation")
            return false
        }
        return hasRussian || hasEnglish
    }
}