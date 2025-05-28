package com.example.messenger.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.messenger.data.models.Message
import com.example.messenger.data.models.User
import com.example.messenger.data.repository.ChatRepository
import com.example.messenger.data.repository.UserRepository

class ChatsViewModel(private val authViewModel: AuthViewModel) : ViewModel() {

    private val chatRepository = ChatRepository()

    private val _chatsState = MutableLiveData<ChatsState>()
    val chatsState: LiveData<ChatsState> = _chatsState

    private val _messagesState = MutableLiveData<MessagesState>()
    val messagesState: LiveData<MessagesState> = _messagesState

    data class ChatsState(
        val chats: List<Triple<String, Message?, Int>> = emptyList(),
        val users: List<User> = emptyList(),
        val error: String? = null
    )

    data class MessagesState(
        val messages: List<Message> = emptyList(),
        val error: String? = null,
        val isLoading: Boolean = false,
        val sendSuccess: Boolean = false
    )

    fun loadChats(userId: String) {
        Log.d("ChatsViewModel", "Loading chats for userId=$userId")
        chatRepository.getChatsWithLastMessage(userId) { chatPairs ->
            if (chatPairs.isEmpty()) {
                Log.d("ChatsViewModel", "No chats found for userId=$userId")
                _chatsState.value = ChatsState(chats = emptyList())
                return@getChatsWithLastMessage
            }
            val chatTriples = mutableListOf<Triple<String, Message?, Int>>()
            chatPairs.forEach { (chatId, lastMessage) ->
                chatRepository.getUnreadCount(userId, chatId) { unreadCount ->
                    chatTriples.add(Triple(chatId, lastMessage, unreadCount))
                    if (chatTriples.size == chatPairs.size) {
                        Log.d("ChatsViewModel", "Chats loaded: ${chatTriples.size}")
                        _chatsState.value = ChatsState(chats = chatTriples)
                        val userIds = chatPairs.map { it.first }.distinct()
                        val userRepository = UserRepository()
                        userRepository.getUsersByIds(userIds) { users ->
                            Log.d("ChatsViewModel", "Users loaded for chats: ${users.size}")
                            _chatsState.value = _chatsState.value?.copy(users = users)
                        }
                    }
                }
            }
        }
    }

    fun loadMessages(senderId: String, receiverId: String) {
        Log.d("ChatsViewModel", "Loading messages between senderId=$senderId and receiverId=$receiverId")
        chatRepository.getMessages(senderId, receiverId) { messages ->
            _messagesState.value = MessagesState(messages = messages)
        }
    }

    fun sendMessage(message: Message) {
        Log.d("ChatsViewModel", "Sending message: ${message.content}")
        _messagesState.value = MessagesState(isLoading = true)
        chatRepository.sendMessage(message) { success, error ->
            if (success) {
                Log.d("ChatsViewModel", "Message sent successfully")
                _messagesState.value = MessagesState(sendSuccess = true)
                val userId = authViewModel.getCurrentUserId()
                userId?.let { loadChats(it) }
            } else {
                Log.e("ChatsViewModel", "Error sending message: $error")
                _messagesState.value = MessagesState(error = error)
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
                _chatsState.value = _chatsState.value?.copy(error = error)
            }
        }
    }
}