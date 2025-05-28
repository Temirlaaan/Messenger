package com.example.messenger.ui.contacts

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messenger.R
import com.example.messenger.ui.chat.ChatActivity
import com.example.messenger.viewmodel.AuthViewModel
import com.example.messenger.viewmodel.ContactsViewModel
import com.example.messenger.databinding.FragmentContactsBinding

class FragmentContacts : Fragment(R.layout.fragment_contacts) {

    private lateinit var binding: FragmentContactsBinding
    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var authViewModel: AuthViewModel
    private lateinit var contactsAdapter: ContactsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentContactsBinding.bind(view)
        contactsViewModel = ViewModelProvider(this).get(ContactsViewModel::class.java)
        authViewModel = ViewModelProvider(requireActivity()).get(AuthViewModel::class.java)

        contactsAdapter = ContactsAdapter { contactId ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("receiverId", contactId)
            }
            startActivity(intent)
        }

        binding.contactsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = contactsAdapter
        }

        contactsViewModel.contactsState.observe(viewLifecycleOwner) { state ->
            Log.d("FragmentContacts", "Contacts state updated: contacts=${state.contacts.size}, error=${state.error}")
            Log.d("FragmentContacts", "Contacts list: ${state.contacts.map { it.username }}")
            contactsAdapter.submitList(state.contacts)
            if (state.error != null) {
                Toast.makeText(requireContext(), "Ошибка загрузки контактов: ${state.error}", Toast.LENGTH_LONG).show()
            }
            if (state.contacts.isEmpty()) {
                Toast.makeText(requireContext(), "Контакты не найдены", Toast.LENGTH_SHORT).show()
            }
        }

        val userId = authViewModel.getCurrentUserId()
        if (userId != null) {
            Log.d("FragmentContacts", "Loading contacts for userId=$userId")
            contactsViewModel.loadContacts(userId)
        } else {
            Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_LONG).show()
        }
    }
}