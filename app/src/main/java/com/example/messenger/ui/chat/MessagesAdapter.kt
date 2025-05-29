package com.example.messenger.ui.chat

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
        return getItem(position).timestamp
    }

    inner class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            // Настройка контента сообщения
            if (message.type == "image" && message.imageUrl != null) {
                binding.messageText.visibility = View.GONE
                binding.messageImage.visibility = View.VISIBLE

                Glide.with(binding.root.context)
                    .load(message.imageUrl)
                    .into(binding.messageImage)

                // Добавляем клик для открытия фото в полном размере
                binding.messageImage.setOnClickListener {
                    val intent = Intent(binding.root.context, FullScreenImageActivity::class.java)
                    intent.putExtra("imageUrl", message.imageUrl)
                    binding.root.context.startActivity(intent)
                }
            } else {
                binding.messageText.visibility = View.VISIBLE
                binding.messageImage.visibility = View.GONE
                binding.messageText.text = message.content

                // Убираем клик для текстовых сообщений
                binding.messageImage.setOnClickListener(null)
            }

            binding.timestampText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.getTimestampAsDate())

            // Правильная настройка выравнивания сообщений
            val rootLayoutParams = binding.root.layoutParams as RecyclerView.LayoutParams

            if (message.senderId == currentUserId) {
                // Мои сообщения - справа
                binding.messageContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#E1FFC7")
                )
                rootLayoutParams.setMargins(100, 8, 16, 8) // Большой отступ слева
                binding.root.layoutParams = rootLayoutParams

                // Убираем RTL, используем gravity
                binding.messageContainer.layoutDirection = View.LAYOUT_DIRECTION_LTR

                // Выравнивание родительского элемента
                if (binding.root is android.widget.FrameLayout) {
                    val containerParams = binding.messageContainer.layoutParams as android.widget.FrameLayout.LayoutParams
                    containerParams.gravity = android.view.Gravity.END
                    binding.messageContainer.layoutParams = containerParams
                }
            } else {
                // Сообщения собеседника - слева
                binding.messageContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#FFFFFF")
                )
                rootLayoutParams.setMargins(16, 8, 100, 8) // Большой отступ справа
                binding.root.layoutParams = rootLayoutParams

                binding.messageContainer.layoutDirection = View.LAYOUT_DIRECTION_LTR

                if (binding.root is android.widget.FrameLayout) {
                    val containerParams = binding.messageContainer.layoutParams as android.widget.FrameLayout.LayoutParams
                    containerParams.gravity = android.view.Gravity.START
                    binding.messageContainer.layoutParams = containerParams
                }
            }
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