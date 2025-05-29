package com.example.messenger.ui.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.messenger.R
import com.example.messenger.data.models.User
import com.example.messenger.databinding.ItemContactBinding

class ContactsAdapter(
    private val onContactClick: (String) -> Unit
) : ListAdapter<User, ContactsAdapter.ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.contactNameText.text = user.username.ifEmpty { user.email }
            binding.statusText.text = when (user.status) {
                "online" -> "Онлайн"
                "offline" -> "Оффлайн"
                else -> "Неизвестно"
            }

            // Загружаем аватар контакта
            if (!user.profileImageUrl.isNullOrEmpty()) {
                Glide.with(binding.profilePicture.context)
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.ic_profile_picture)
                    .error(R.drawable.ic_profile_picture)
                    .circleCrop()
                    .into(binding.profilePicture)
            } else {
                binding.profilePicture.setImageResource(R.drawable.ic_profile_picture)
            }

            binding.root.setOnClickListener { onContactClick(user.uid) }
        }
    }
}

class ContactDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean = oldItem.uid == newItem.uid
    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
}