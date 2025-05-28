package com.example.messenger.ui.chat

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messenger.R
import com.example.messenger.data.models.Message
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
            binding.messagesRecyclerView.layoutManager = LinearLayoutManager(this)
            binding.messagesRecyclerView.adapter = messagesAdapter

            chatsViewModel.loadMessages(senderId, receiverId!!)
            chatsViewModel.messagesState.observe(this) { state ->
                Log.d("ChatActivity", "Messages state updated: messages=${state.messages.size}, error=${state.error}")
                messagesAdapter.submitList(state.messages)
                if (state.messages.isNotEmpty()) {
                    binding.messagesRecyclerView.scrollToPosition(state.messages.size - 1)
                }
                if (state.error != null) {
                    Toast.makeText(this, "Ошибка загрузки сообщений: ${state.error}", Toast.LENGTH_LONG).show()
                }
                if (state.sendSuccess) {
                    chatsViewModel.loadMessages(senderId, receiverId!!)
                }
            }

            binding.sendButton.setOnClickListener {
                val content = binding.messageEditText.text.toString().trim()
                if (content.isNotEmpty() && receiverId != null) {
                    val message = Message(
                        senderId = senderId,
                        receiverId = receiverId!!,
                        content = content,
                        timestamp = System.currentTimeMillis(),
                        isRead = false
                    )
                    chatsViewModel.sendMessage(message)
                    binding.messageEditText.text?.clear()
                }
            }
        } else {
            Toast.makeText(this, "Ошибка: пользователь или получатель не определен", Toast.LENGTH_LONG).show()
        }
    }
}