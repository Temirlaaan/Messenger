package com.example.messenger.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.messenger.data.models.Message
import com.example.messenger.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter(private val currentUserId: String) :
    ListAdapter<Message, MessagesAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long {
        // Используем timestamp как уникальный ID
        return getItem(position).timestamp
    }

    inner class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.messageText.text = message.content
            binding.timestampText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.getTimestampAsDate())

            // Настраиваем выравнивание и цвет фона через LayoutParams
            val layoutParams = binding.messageContainer.layoutParams as LinearLayout.LayoutParams
            if (message.senderId == currentUserId) {
                // Свои сообщения (справа)
                binding.messageContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#E1FFC7") // Светло-зеленый
                )
                layoutParams.gravity = android.view.Gravity.END
                layoutParams.setMargins(64, 0, 8, 0) // Отступы: слева 64dp, справа 8dp
                binding.messageContainer.layoutDirection = View.LAYOUT_DIRECTION_RTL
            } else {
                // Чужие сообщения (слева)
                binding.messageContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#FFFFFF") // Белый
                )
                layoutParams.gravity = android.view.Gravity.START
                layoutParams.setMargins(8, 0, 64, 0) // Отступы: слева 8dp, справа 64dp
                binding.messageContainer.layoutDirection = View.LAYOUT_DIRECTION_LTR
            }
            binding.messageContainer.layoutParams = layoutParams
        }
    }
}

class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.timestamp == newItem.timestamp && oldItem.senderId == newItem.senderId
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }
}