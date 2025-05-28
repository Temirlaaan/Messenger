package com.example.messenger.data.repository

import android.util.Log
import com.example.messenger.data.models.Message
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatRepository {

    private val db: DatabaseReference = FirebaseDatabase.getInstance().getReference()

    fun getChatsWithLastMessage(userId: String, useSingleEvent: Boolean = false, callback: (List<Pair<String, Message?>>) -> Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chats = snapshot.children.mapNotNull { chatSnapshot ->
                    val chatId = chatSnapshot.key ?: return@mapNotNull null
                    val lastMessage = chatSnapshot.child("lastMessage").getValue(Message::class.java)
                    val otherUserId = chatId.replace(userId, "").replace("_", "")
                    otherUserId to lastMessage
                }
                Log.d("ChatRepository", "Chats loaded for userId=$userId: ${chats.size}")
                callback(chats)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Error getting chats: ${error.message}")
                callback(emptyList())
            }
        }

        if (useSingleEvent) {
            db.child("chats").child(userId).addListenerForSingleValueEvent(listener)
        } else {
            db.child("chats").child(userId).addValueEventListener(listener)
        }
    }

    fun getUnreadCount(userId: String, chatId: String, callback: (Int) -> Unit) {
        db.child("messages").child(chatId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val unreadCount = snapshot.children.count { messageSnapshot ->
                    val message = messageSnapshot.getValue(Message::class.java)
                    message?.receiverId == userId && message.isRead == false
                }
                Log.d("ChatRepository", "Unread count for $chatId: $unreadCount")
                callback(unreadCount)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Error getting unread count: ${error.message}")
                callback(0)
            }
        })
    }

    fun getMessages(senderId: String, receiverId: String, callback: (List<Message>) -> Unit) {
        val chatId = if (senderId < receiverId) "${senderId}_${receiverId}" else "${receiverId}_${senderId}"
        db.child("messages").child(chatId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                    .sortedBy { it.timestamp }
                Log.d("ChatRepository", "Messages loaded for chatId=$chatId: ${messages.size}")
                callback(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Error getting messages: ${error.message}")
                callback(emptyList())
            }
        })
    }

    fun sendMessage(message: Message, callback: (Boolean, String?) -> Unit) {
        val chatId = if (message.senderId < message.receiverId) "${message.senderId}_${message.receiverId}" else "${message.receiverId}_${message.senderId}"
        val messageRef = db.child("messages").child(chatId).push()
        messageRef.setValue(message).addOnSuccessListener {
            updateLastMessage(chatId, message)
            callback(true, null)
        }.addOnFailureListener { error ->
            Log.e("ChatRepository", "Error sending message: ${error.message}")
            callback(false, error.message)
        }
    }

    private fun updateLastMessage(chatId: String, message: Message) {
        val senderChatRef = db.child("chats").child(message.senderId).child(chatId)
        val receiverChatRef = db.child("chats").child(message.receiverId).child(chatId)
        senderChatRef.child("lastMessage").setValue(message)
        receiverChatRef.child("lastMessage").setValue(message)
    }

    fun markAsRead(senderId: String, receiverId: String, currentUserId: String, callback: (Boolean, String?) -> Unit) {
        val chatId = if (senderId < receiverId) "${senderId}_${receiverId}" else "${receiverId}_${senderId}"
        db.child("messages").child(chatId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { messageSnapshot ->
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message?.receiverId == currentUserId && message.isRead == false) {
                        messageSnapshot.ref.child("isRead").setValue(true)
                    }
                }
                callback(true, null)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Error marking as read: ${error.message}")
                callback(false, error.message)
            }
        })
    }
}