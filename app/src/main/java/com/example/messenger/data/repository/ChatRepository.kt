package com.example.messenger.data.repository

import android.util.Log
import com.example.messenger.data.models.Message
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatRepository {

    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference

    fun getMessages(senderId: String, receiverId: String, callback: (List<Message>) -> Unit) {
        val chatId = if (senderId < receiverId) "${senderId}_${receiverId}" else "${receiverId}_${senderId}"
        db.child("messages").child(chatId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                    .sortedBy { it.timestamp }
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
        val messageMap = mapOf(
            "senderId" to message.senderId,
            "receiverId" to message.receiverId,
            "content" to message.content,
            "timestamp" to message.timestamp,
            "isRead" to message.isRead,
            "type" to message.type,
            "imageUrl" to message.imageUrl,
            "isEncrypted" to message.isEncrypted,
            "encryptedAESKey" to message.encryptedAESKey,
            "iv" to message.iv,
            "messageHash" to message.messageHash,
            "timeSlot" to message.timeSlot
        )
        messageRef.setValue(messageMap).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true, null)
            } else {
                callback(false, task.exception?.message)
            }
        }
    }

    fun getChatsWithLastMessage(userId: String, useSingleEvent: Boolean, callback: (List<Pair<String, Message?>>) -> Unit) {
        val chatsRef = db.child("messages")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatPairs = mutableListOf<Pair<String, Message?>>()
                for (chatSnapshot in snapshot.children) {
                    val chatId = chatSnapshot.key ?: continue
                    if (chatId.contains(userId)) {
                        val otherUserId = chatId.split("_").find { it != userId } ?: continue
                        val lastMessage = chatSnapshot.children
                            .mapNotNull { it.getValue(Message::class.java) }
                            .maxByOrNull { it.timestamp }
                        chatPairs.add(Pair(otherUserId, lastMessage))
                    }
                }
                callback(chatPairs)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Error getting chats: ${error.message}")
                callback(emptyList())
            }
        }
        if (useSingleEvent) {
            chatsRef.addListenerForSingleValueEvent(listener)
        } else {
            chatsRef.addValueEventListener(listener)
        }
    }

    fun getUnreadCount(userId: String, otherUserId: String, callback: (Int) -> Unit) {
        val chatId = if (userId < otherUserId) "${userId}_${otherUserId}" else "${otherUserId}_${userId}"
        db.child("messages").child(chatId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val unreadCount = snapshot.children
                    .mapNotNull { it.getValue(Message::class.java) }
                    .count { it.receiverId == userId && !it.isRead }
                callback(unreadCount)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Error getting unread count: ${error.message}")
                callback(0)
            }
        })
    }

    fun markAsRead(senderId: String, receiverId: String, currentUserId: String, callback: (Boolean, String?) -> Unit) {
        val chatId = if (senderId < receiverId) "${senderId}_${receiverId}" else "${receiverId}_${senderId}"
        db.child("messages").child(chatId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updates = mutableMapOf<String, Any>()
                snapshot.children.forEach { messageSnapshot ->
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message?.receiverId == currentUserId && !message.isRead) {
                        updates["${messageSnapshot.key}/isRead"] = true
                    }
                }
                if (updates.isNotEmpty()) {
                    db.child("messages").child(chatId).updateChildren(updates).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            callback(true, null)
                        } else {
                            callback(false, task.exception?.message)
                        }
                    }
                } else {
                    callback(true, null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false, error.message)
            }
        })
    }
}