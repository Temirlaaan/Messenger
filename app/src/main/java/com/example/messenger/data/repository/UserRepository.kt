package com.example.messenger.data.repository

import android.util.Log
import com.example.messenger.data.models.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserRepository {

    private val db = FirebaseDatabase.getInstance().reference

    fun saveUser(userId: String, userMap: Map<String, String>) {
        db.child("users").child(userId).setValue(userMap)
            .addOnSuccessListener { Log.d("UserRepository", "User saved: $userId") }
            .addOnFailureListener { e -> Log.e("UserRepository", "Error saving user: ${e.message}") }
    }

    fun getUser(userId: String, callback: (User) -> Unit) {
        db.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)?.copy(uid = userId) ?: User()
                    Log.d("UserRepository", "User loaded: $user")
                    callback(user)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserRepository", "Error getting user: ${error.message}")
                    callback(User())
                }
            })
    }

    fun getUsersByIds(userIds: List<String>, callback: (List<User>) -> Unit) {
        if (userIds.isEmpty()) {
            Log.d("UserRepository", "No user IDs to load")
            callback(emptyList())
            return
        }
        val users = mutableListOf<User>()
        userIds.forEach { userId ->
            db.child("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)?.copy(uid = userId)
                        if (user != null) users.add(user)
                        if (users.size == userIds.size) {
                            Log.d("UserRepository", "Users loaded by IDs: ${users.size}, IDs=$userIds")
                            callback(users)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("UserRepository", "Error getting user by ID $userId: ${error.message}")
                        if (users.size == userIds.size) callback(users)
                    }
                })
        }
    }

    fun getAllUsers(exceptUserId: String, callback: (List<User>) -> Unit) {
        db.child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val allUsers = snapshot.children.mapNotNull { it.getValue(User::class.java)?.copy(uid = it.key!!) }
                    val filteredUsers = allUsers.filter { it.uid != exceptUserId }
                    Log.d("UserRepository", "Filtered users (except $exceptUserId): ${filteredUsers.size}")
                    callback(filteredUsers)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserRepository", "Error getting all users: ${error.message}")
                    callback(emptyList())
                }
            })
    }
}