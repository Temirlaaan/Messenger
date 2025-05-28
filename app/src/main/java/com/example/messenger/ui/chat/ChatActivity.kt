package com.example.messenger.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.messenger.R
import com.example.messenger.data.models.Message
import com.example.messenger.data.models.User
import com.example.messenger.data.repository.UserRepository
import com.example.messenger.databinding.ActivityChatBinding
import com.example.messenger.viewmodel.AuthViewModel
import com.example.messenger.viewmodel.ChatsViewModel
import com.example.messenger.viewmodel.ChatsViewModelFactory

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatsViewModel: ChatsViewModel
    private lateinit var authViewModel: AuthViewModel
    private lateinit var messagesAdapter: MessagesAdapter
    private var receiverId: String? = null
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)
        chatsViewModel = ViewModelProvider(this, ChatsViewModelFactory(authViewModel)).get(ChatsViewModel::class.java)
        receiverId = intent.getStringExtra("receiverId")

        val senderId = authViewModel.getCurrentUserId()
        if (senderId != null && receiverId != null) {
            messagesAdapter = MessagesAdapter(senderId)
            // Устанавливаем стабильные ID до привязки адаптера к RecyclerView
            messagesAdapter.setHasStableIds(true)

            val layoutManager = LinearLayoutManager(this)
            layoutManager.stackFromEnd = true // Начинаем с конца списка
            binding.messagesRecyclerView.layoutManager = layoutManager
            binding.messagesRecyclerView.adapter = messagesAdapter

            // Загружаем данные пользователя (имя и статус)
            userRepository.getUser(receiverId!!) { user ->
                updateUserInfo(user)
            }

            // Проверяем, находится ли пользователь внизу списка
            var isAtBottom = true
            binding.messagesRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    isAtBottom = layoutManager.findLastCompletelyVisibleItemPosition() >= messagesAdapter.itemCount - 1
                }
            })

            // Загружаем сообщения
            chatsViewModel.loadMessages(senderId, receiverId!!)
            chatsViewModel.messagesState.observe(this) { state ->
                Log.d("ChatActivity", "Messages state updated: messages=${state.messages.size}, error=${state.error}, isLoading=${state.isLoading}")
                if (state.isLoading) {
                    // Показываем ProgressBar только при начальной загрузке
                    if (messagesAdapter.itemCount == 0) {
                        binding.loadingProgressBar.visibility = View.VISIBLE
                        binding.headerLayout.visibility = View.GONE
                        binding.messagesRecyclerView.visibility = View.GONE
                        binding.messageInputLayout.visibility = View.GONE
                    }
                } else {
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.headerLayout.visibility = View.VISIBLE
                    binding.messagesRecyclerView.visibility = View.VISIBLE
                    binding.messageInputLayout.visibility = View.VISIBLE
                }

                // Обновляем список сообщений
                val previousItemCount = messagesAdapter.itemCount
                val wasAtBottom = isAtBottom
                messagesAdapter.submitList(state.messages.distinctBy { it.timestamp to it.content }) {
                    val newItemCount = messagesAdapter.itemCount
                    if (newItemCount > previousItemCount && wasAtBottom) {
                        binding.messagesRecyclerView.scrollToPosition(newItemCount - 1)
                    } else if (newItemCount > previousItemCount) {
                        Toast.makeText(this, "Новое сообщение", Toast.LENGTH_SHORT).show()
                    }
                }

                if (state.error != null) {
                    Toast.makeText(this, "Ошибка загрузки сообщений: ${state.error}", Toast.LENGTH_LONG).show()
                }
            }

            // Обработка отправки текстового сообщения
            binding.sendButton.setOnClickListener {
                sendMessage(senderId)
            }

            // Обработка отправки изображения (заглушка для будущей функции)
            binding.sendImageButton.setOnClickListener {
                Toast.makeText(this, "Функция отправки изображения пока не реализована", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Ошибка: пользователь или получатель не определен", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendMessage(senderId: String) {
        val content = binding.messageEditText.text.toString().trim()
        if (content.isNotEmpty() && receiverId != null) {
            val message = Message(
                senderId = senderId,
                receiverId = receiverId!!,
                content = content,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
            // Отключаем кнопку на время отправки
            binding.sendButton.isEnabled = false
            chatsViewModel.sendMessage(message)
            binding.messageEditText.text?.clear()
            // Включаем кнопку после отправки (в observe)
            chatsViewModel.messagesState.observe(this) { state ->
                if (!state.isLoading) {
                    binding.sendButton.isEnabled = true
                }
            }
        }
    }

    private fun updateUserInfo(user: User) {
        binding.txtviewUser.text = user.username.ifEmpty { user.email }
        binding.status.text = when (user.status) {
            "online" -> "Онлайн"
            "offline" -> "Оффлайн"
            else -> "Неизвестно"
        }
    }
}