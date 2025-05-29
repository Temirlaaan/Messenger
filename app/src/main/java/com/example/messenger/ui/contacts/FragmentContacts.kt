package com.example.messenger.ui.contacts

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messenger.R
import com.example.messenger.data.models.User
import com.example.messenger.ui.chat.ChatActivity
import com.example.messenger.viewmodel.AuthViewModel
import com.example.messenger.viewmodel.ContactsViewModel
import com.example.messenger.databinding.FragmentContactsBinding

class FragmentContacts : Fragment(R.layout.fragment_contacts) {

    private lateinit var binding: FragmentContactsBinding
    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var authViewModel: AuthViewModel
    private lateinit var contactsAdapter: ContactsAdapter
    private var allContacts: List<User> = emptyList() // Сохраняем полный список для фильтрации

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentContactsBinding.bind(view)
        contactsViewModel = ViewModelProvider(this).get(ContactsViewModel::class.java)
        authViewModel = ViewModelProvider(requireActivity()).get(AuthViewModel::class.java)

        // Инициализируем адаптер один раз
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

        // Настраиваем слушатель для EditText
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterContacts(s.toString())
            }
        })

        contactsViewModel.contactsState.observe(viewLifecycleOwner) { state ->
            Log.d("FragmentContacts", "Contacts state updated: contacts=${state.contacts.size}, error=${state.error}, isLoading=${state.isLoading}")
            Log.d("FragmentContacts", "Contacts list: ${state.contacts.map { it.username }}")
            if (state.isLoading) {
                binding.loadingProgressBar.visibility = View.VISIBLE
                binding.contactsTitle.visibility = View.GONE
                binding.contactsRecyclerView.visibility = View.GONE
            } else {
                binding.loadingProgressBar.visibility = View.GONE
                if (state.contacts.isNotEmpty()) {
                    binding.contactsTitle.visibility = View.VISIBLE
                    binding.contactsRecyclerView.visibility = View.VISIBLE
                    allContacts = state.contacts // Сохраняем полный список
                    filterContacts(binding.searchEditText.text.toString()) // Применяем текущий фильтр
                } else {
                    binding.contactsTitle.visibility = View.GONE
                    binding.contactsRecyclerView.visibility = View.GONE
                    allContacts = emptyList()
                    contactsAdapter.submitList(emptyList())
                }
            }
            if (state.error != null) {
                Toast.makeText(requireContext(), "Ошибка загрузки контактов: ${state.error}", Toast.LENGTH_LONG).show()
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

    private fun filterContacts(query: String) {
        val filteredList = if (query.isEmpty()) {
            allContacts
        } else {
            allContacts.filter {
                it.username.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
            }
        }
        contactsAdapter.submitList(filteredList)
        binding.contactsRecyclerView.scrollToPosition(0) // Прокручиваем к началу списка после фильтрации
    }
}