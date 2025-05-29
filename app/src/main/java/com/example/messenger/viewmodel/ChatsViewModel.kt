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
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class ChatsViewModel(private val authViewModel: AuthViewModel) : ViewModel() {

    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()
    private val storage = FirebaseStorage.getInstance()

    private val _chatsState = MutableLiveData<ChatsState>()
    val chatsState: LiveData<ChatsState> = _chatsState

    private val _messagesState = MutableLiveData<MessagesState>()
    val messagesState: LiveData<MessagesState> = _messagesState

    private var cachedChats: List<Triple<String, Message?, Int>>? = null
    private var cachedUsers: List<User>? = null
    private var cachedMessages: List<Message>? = null

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
                    chatTriples.add(Triple(chatId, lastMessage, unreadCount))
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
            if (cachedMessages != messages) {
                cachedMessages = messages
                _messagesState.value = MessagesState(messages = messages, isLoading = false)
            } else {
                _messagesState.value = MessagesState(messages = cachedMessages!!, isLoading = false)
            }
        }
    }

    fun sendMessage(message: Message) {
        Log.d("ChatsViewModel", "Sending message: ${message.content}")

        val currentState = _messagesState.value ?: MessagesState()
        _messagesState.value = currentState.copy(isSending = true)

        chatRepository.sendMessage(message) { success, error ->
            if (success) {
                Log.d("ChatsViewModel", "Message sent successfully")
                _messagesState.value = MessagesState(
                    messages = cachedMessages ?: emptyList(),
                    isLoading = false,
                    sendSuccess = true,
                    isSending = false
                )
                val userId = authViewModel.getCurrentUserId()
                userId?.let { loadChats(it) }
            } else {
                Log.e("ChatsViewModel", "Error sending message: $error")
                _messagesState.value = MessagesState(
                    error = error,
                    isLoading = false,
                    isSending = false
                )
            }
        }
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
                    content = "Image", // Текст для отображения в списке чатов
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
        Log.d("ChatsViewModel", "Cache cleared")
    }
}