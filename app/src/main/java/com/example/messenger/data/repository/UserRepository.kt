package com.example.messenger.data.repository

    import android.util.Log
    import com.google.firebase.database.DataSnapshot
    import com.google.firebase.database.DatabaseError
    import com.google.firebase.database.FirebaseDatabase
    import com.google.firebase.database.ValueEventListener
    import com.example.messenger.data.models.User

    class UserRepository {

        private val db = FirebaseDatabase.getInstance().reference

        fun getAllUsers(exceptUserId: String? = null, callback: (List<User>) -> Unit) {
            db.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val users = snapshot.children.mapNotNull { data ->
                        val userId = data.key
                        if (userId != exceptUserId) {
                            val userData = data.value as? Map<String, Any?> ?: emptyMap()
                            User(
                                uid = userId ?: "",
                                username = userData["username"] as? String ?: "",
                                email = userData["email"] as? String ?: "",
                                status = userData["status"] as? String ?: "offline",
                                profileImageUrl = userData["profileImageUrl"] as? String
                            )
                        } else {
                            null // Исключаем пользователя с exceptUserId
                        }
                    }
                    callback(users)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserRepository", "Error getting users: ${error.message}")
                    callback(emptyList())
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
                            val userData = snapshot.value as? Map<String, Any?> ?: emptyMap<String, Any?>()
                            val user = User(
                                uid = userId,
                                username = userData["username"] as? String ?: "",
                                email = userData["email"] as? String ?: "",
                                status = userData["status"] as? String ?: "offline",
                                profileImageUrl = userData["profileImageUrl"] as? String
                            )
                            if (user.uid.isNotEmpty()) users.add(user)
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

        fun getUser(userId: String, callback: (User) -> Unit) {
            db.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userData = snapshot.value as? Map<String, Any?> ?: emptyMap()
                    val user = User(
                        uid = userId,
                        username = userData["username"] as? String ?: "",
                        email = userData["email"] as? String ?: "",
                        status = userData["status"] as? String ?: "offline",
                        profileImageUrl = userData["profileImageUrl"] as? String
                    )
                    callback(user)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserRepository", "Error getting user: ${error.message}")
                    callback(User())
                }
            })
        }

        fun saveUser(userId: String, userMap: Map<String, Any?>) {
            db.child("users").child(userId).updateChildren(userMap)
                .addOnFailureListener { e ->
                    Log.e("UserRepository", "Error saving user: ${e.message}")
                }
        }
    }