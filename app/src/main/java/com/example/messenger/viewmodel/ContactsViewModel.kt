package com.example.messenger.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.messenger.data.models.User
import com.example.messenger.data.repository.UserRepository

class ContactsViewModel : ViewModel() {

    private val userRepository = UserRepository()

    private val _contactsState = MutableLiveData<ContactsState>()
    val contactsState: LiveData<ContactsState> = _contactsState

    data class ContactsState(
        val contacts: List<User> = emptyList(),
        val error: String? = null,
        val isLoading: Boolean = false // Добавлено
    )

    fun loadContacts(userId: String) {
        Log.d("ContactsViewModel", "Loading contacts for userId=$userId")
        _contactsState.value = ContactsState(isLoading = true) // Устанавливаем состояние загрузки
        userRepository.getAllUsers(userId) { users ->
            Log.d("ContactsViewModel", "Contacts loaded: ${users.size}")
            _contactsState.value = ContactsState(contacts = users, isLoading = false)
        }
    }
}