package com.example.messenger.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
    private var isInitialLoad = true

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val senderId = authViewModel.getCurrentUserId()
            if (senderId != null && receiverId != null) {
                chatsViewModel.uploadAndSendImage(senderId, receiverId!!, uri) { success, error ->
                    if (!success) {
                        Toast.makeText(this, "Ошибка загрузки изображения: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)
        chatsViewModel = ViewModelProvider(this, ChatsViewModelFactory(authViewModel, this)).get(ChatsViewModel::class.java)
        receiverId = intent.getStringExtra("receiverId")

        val senderId = authViewModel.getCurrentUserId()
        if (senderId != null && receiverId != null) {
            messagesAdapter = MessagesAdapter(senderId) { message, position ->
                chatsViewModel.translateMessage(message, position)
            }
            messagesAdapter.setHasStableIds(true)

            val layoutManager = LinearLayoutManager(this)
            layoutManager.stackFromEnd = true
            layoutManager.reverseLayout = false
            binding.messagesRecyclerView.layoutManager = layoutManager
            binding.messagesRecyclerView.adapter = messagesAdapter

            userRepository.getUser(receiverId!!) { user ->
                updateUserInfo(user)
            }

            var isAtBottom = true
            binding.messagesRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    isAtBottom = layoutManager.findLastCompletelyVisibleItemPosition() >= messagesAdapter.itemCount - 1
                }
            })

            chatsViewModel.loadMessages(senderId, receiverId!!)
            chatsViewModel.messagesState.observe(this) { state ->
                Log.d("ChatActivity", "Messages state updated: messages=${state.messages.size}, error=${state.error}, isLoading=${state.isLoading}, isSending=${state.isSending}")

                if (state.isLoading && isInitialLoad) {
                    binding.loadingProgressBar.visibility = View.VISIBLE
                    binding.headerLayout.visibility = View.GONE
                    binding.messagesRecyclerView.visibility = View.GONE
                    binding.messageInputLayout.visibility = View.GONE
                } else {
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.headerLayout.visibility = View.VISIBLE
                    binding.messagesRecyclerView.visibility = View.VISIBLE
                    binding.messageInputLayout.visibility = View.VISIBLE

                    if (isInitialLoad) {
                        isInitialLoad = false
                    }
                }

                binding.sendButton.isEnabled = !state.isSending
                binding.sendImageButton.isEnabled = !state.isSending

                val previousItemCount = messagesAdapter.itemCount
                val wasAtBottom = isAtBottom

                messagesAdapter.submitList(state.messages) {
                    val newItemCount = messagesAdapter.itemCount
                    if (newItemCount > previousItemCount && wasAtBottom) {
                        binding.messagesRecyclerView.scrollToPosition(newItemCount - 1)
                    } else if (newItemCount > previousItemCount && !isInitialLoad) {
                        Toast.makeText(this, "Новое сообщение", Toast.LENGTH_SHORT).show()
                    }
                }

                if (state.error != null) {
                    Toast.makeText(this, "Ошибка загрузки сообщений: ${state.error}", Toast.LENGTH_LONG).show()
                }
            }

            chatsViewModel.translationState.observe(this) { state ->
                state?.let {
                    if (it.isTranslating) {
                        binding.loadingProgressBar.visibility = View.VISIBLE
                    } else {
                        binding.loadingProgressBar.visibility = View.GONE
                        it.error?.let { error ->
                            Toast.makeText(this, "Ошибка перевода: $error", Toast.LENGTH_SHORT).show()
                        }
                        chatsViewModel.messagesState.value?.messages?.let { messages ->
                            messagesAdapter.submitList(messages)
                        }
                    }
                }
            }

            // Обработчик кнопки "Назад"
            binding.backButton.setOnClickListener {
                finish() // Завершает активность и возвращает к предыдущему экрану
            }

            binding.sendButton.setOnClickListener {
                sendMessage(senderId)
            }

            binding.sendImageButton.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }
        } else {
            Toast.makeText(this, "Ошибка: пользователь или получатель не определен", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendMessage(senderId: String) {
        val content = binding.messageEditText.text.toString().trim()
        if (content.isNotEmpty() && receiverId != null) {
            val timestamp = System.currentTimeMillis()
            val message = Message(
                senderId = senderId,
                receiverId = receiverId!!,
                content = content,
                timestamp = timestamp,
                isRead = false,
                type = "text",
                timeSlot = timestamp / 3600_000L
            )

            chatsViewModel.sendMessage(message)
            binding.messageEditText.text?.clear()
        }
    }

    private fun updateUserInfo(user: User) {
        binding.txtviewUser.text = user.username.ifEmpty { user.email }
        binding.status.text = when (user.status) {
            "online" -> "Онлайн"
            "offline" -> "Оффлайн"
            else -> "Неизвестно"
        }

        if (user.profileImageUrl?.isNotEmpty() == true) {
            Glide.with(this)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_profile_picture)
                .error(R.drawable.ic_profile_picture)
                .circleCrop()
                .into(binding.profileImageView)
        } else {
            binding.profileImageView.setImageResource(R.drawable.ic_profile_picture)
        }
    }
}