package com.example.messenger.ui.chats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.messenger.R
import com.example.messenger.data.models.Message
import com.example.messenger.data.models.User
import com.example.messenger.databinding.ItemChatBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatsAdapter(
    private val onChatClick: (String) -> Unit,
    private var users: List<User> = emptyList() // Изначально пустой список
) : ListAdapter<Triple<String, Message?, Int>, ChatsAdapter.ChatViewHolder>(ChatDiffCallback()) {

    // Метод для обновления списка пользователей
    fun updateUsers(newUsers: List<User>) {
        this.users = newUsers
        notifyDataSetChanged() // Обновляем UI после изменения списка пользователей
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(private val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: Triple<String, Message?, Int>) {
            val userId = chat.first
            val lastMessage = chat.second
            val unreadCount = chat.third
            val user = users.find { it.uid == userId }

            binding.chatNameText.text = user?.username?.ifEmpty { user.email } ?: "User_$userId"
            binding.lastMessageText.text = lastMessage?.content?.takeIf { it.isNotEmpty() }?.let { "Last: $it" } ?: "No messages yet"
            binding.timestampText.text = lastMessage?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it.getTimestampAsDate()) } ?: ""
            binding.statusText.text = when (user?.status) {
                "online" -> "Онлайн"
                "offline" -> "Оффлайн"
                else -> "Неизвестно"
            }

            if (unreadCount > 0) {
                binding.lastMessageText.text = "${binding.lastMessageText.text} (Unread: $unreadCount)"
            }

            // Загружаем аватар пользователя
            user?.let { userInfo ->
                if (userInfo.profileImageUrl?.isNotEmpty() == true) {
                    Glide.with(binding.root.context)
                        .load(userInfo.profileImageUrl)
                        .placeholder(R.drawable.ic_profile_picture)
                        .error(R.drawable.ic_profile_picture)
                        .circleCrop()
                        .into(binding.profileImageView) // Предполагаем, что в item_chat.xml есть ImageView с этим ID
                } else {
                    binding.profileImageView.setImageResource(R.drawable.ic_profile_picture)
                }
            }

            binding.root.setOnClickListener { onChatClick(userId) }
        }
    }
}

class ChatDiffCallback : DiffUtil.ItemCallback<Triple<String, Message?, Int>>() {
    override fun areItemsTheSame(oldItem: Triple<String, Message?, Int>, newItem: Triple<String, Message?, Int>): Boolean = oldItem.first == newItem.first
    override fun areContentsTheSame(oldItem: Triple<String, Message?, Int>, newItem: Triple<String, Message?, Int>): Boolean = oldItem == newItem
}