package com.example.messenger.data.repository

import android.util.Log
import com.example.messenger.data.models.Message
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatRepository {

    private val db = FirebaseDatabase.getInstance().reference

    fun sendMessage(message: Message, callback: (Boolean, String?) -> Unit) {
        val chatId = getChatId(message.senderId, message.receiverId)
        val messageId = db.child("messages").child(chatId).push().key ?: return
        val messageMap = message.copy(id = messageId, timestamp = System.currentTimeMillis()).toMap()

        val updates = mutableMapOf<String, Any>()
        updates["messages/$chatId/$messageId"] = messageMap
        updates["chats/${message.senderId}/$chatId/lastMessage"] = messageMap
        updates["chats/${message.receiverId}/$chatId/lastMessage"] = messageMap

        db.updateChildren(updates)
            .addOnSuccessListener {
                Log.d("ChatRepository", "Message sent, chatId=$chatId, messageId=$messageId")
                callback(true, null)
            }
            .addOnFailureListener { e ->
                Log.e("ChatRepository", "Error sending message: ${e.message}")
                callback(false, e.message)
            }
    }

    fun getMessages(senderId: String, receiverId: String, callback: (List<Message>) -> Unit) {
        val chatId = getChatId(senderId, receiverId)
        db.child("messages").child(chatId)
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
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

    fun getChatsWithLastMessage(userId: String, callback: (List<Pair<String, Message?>>) -> Unit) {
        db.child("chats").child(userId)
            .addValueEventListener(object : ValueEventListener {
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
            })
    }

    fun getUnreadCount(userId: String, receiverId: String, callback: (Int) -> Unit) {
        val chatId = getChatId(userId, receiverId)
        db.child("messages").child(chatId)
            .orderByChild("receiverId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val unreadCount = snapshot.children.count { it.child("isRead").getValue(Boolean::class.java) == false }
                    Log.d("ChatRepository", "Unread count for $receiverId: $unreadCount")
                    callback(unreadCount)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatRepository", "Error getting unread count: ${error.message}")
                    callback(0)
                }
            })
    }

    fun markAsRead(senderId: String, receiverId: String, currentUserId: String, callback: (Boolean, String?) -> Unit) {
        val chatId = getChatId(senderId, receiverId)
        db.child("messages").child(chatId)
            .orderByChild("receiverId").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val updates = mutableMapOf<String, Any>()
                    snapshot.children.forEach { messageSnapshot ->
                        if (messageSnapshot.child("isRead").getValue(Boolean::class.java) == false) {
                            updates["messages/$chatId/${messageSnapshot.key}/isRead"] = true
                        }
                    }
                    if (updates.isEmpty()) {
                        callback(true, null)
                        return
                    }
                    db.updateChildren(updates)
                        .addOnSuccessListener { callback(true, null) }
                        .addOnFailureListener { e ->
                            Log.e("ChatRepository", "Error marking as read: ${e.message}")
                            callback(false, e.message)
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatRepository", "Error marking as read: ${error.message}")
                    callback(false, error.message)
                }
            })
    }

    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }
}

fun Message.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "senderId" to senderId,
        "receiverId" to receiverId,
        "content" to content,
        "timestamp" to timestamp, // Теперь храним как Long
        "isRead" to isRead
    )
}