package com.example.messenger.ui.chats

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messenger.R
import com.example.messenger.data.models.Message
import com.example.messenger.data.models.User
import com.example.messenger.databinding.FragmentChatsBinding
import com.example.messenger.ui.chat.ChatActivity
import com.example.messenger.viewmodel.AuthViewModel
import com.example.messenger.viewmodel.ChatsViewModel
import com.example.messenger.viewmodel.ChatsViewModelFactory

class FragmentChats : Fragment(R.layout.fragment_chats) {

    private lateinit var binding: FragmentChatsBinding
    private lateinit var chatsViewModel: ChatsViewModel
    private lateinit var authViewModel: AuthViewModel
    private lateinit var chatsAdapter: ChatsAdapter
    private var allChats: List<Triple<String, Message?, Int>> = emptyList()
    private var allUsers: List<User> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentChatsBinding.bind(view)
        authViewModel = ViewModelProvider(requireActivity()).get(AuthViewModel::class.java)
        // Передаём контекст через requireContext()
        chatsViewModel = ViewModelProvider(
            requireActivity(),
            ChatsViewModelFactory(authViewModel, requireContext())
        ).get(ChatsViewModel::class.java)

        chatsAdapter = ChatsAdapter({ chatId ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("receiverId", chatId)
            }
            chatsViewModel.markAsRead(authViewModel.getCurrentUserId()!!, chatId)
            startActivity(intent)
        }, emptyList())

        binding.chatsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatsAdapter
        }

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterChats(s.toString())
            }
        })

        chatsViewModel.chatsState.observe(viewLifecycleOwner) { state ->
            if (state.isLoading) {
                binding.loadingProgressBar.visibility = View.VISIBLE
                binding.contactsTitle.visibility = View.GONE
                binding.chatsRecyclerView.visibility = View.GONE
            } else {
                binding.loadingProgressBar.visibility = View.GONE
                if (state.chats.isNotEmpty()) {
                    binding.contactsTitle.visibility = View.VISIBLE
                    binding.chatsRecyclerView.visibility = View.VISIBLE
                    allChats = state.chats
                    allUsers = state.users
                    chatsAdapter.updateUsers(allUsers)
                    filterChats(binding.searchEditText.text.toString())
                } else {
                    binding.contactsTitle.visibility = View.GONE
                    binding.chatsRecyclerView.visibility = View.GONE
                    allChats = emptyList()
                    allUsers = emptyList()
                    chatsAdapter.submitList(emptyList())
                }
            }
            if (state.error != null) {
                Toast.makeText(requireContext(), "Ошибка загрузки чатов: ${state.error}", Toast.LENGTH_LONG).show()
            }
        }

        val userId = authViewModel.getCurrentUserId()
        if (userId != null) {
            chatsViewModel.loadChats(userId)
        } else {
            Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_LONG).show()
        }
    }

    private fun filterChats(query: String) {
        val filteredChats = if (query.isEmpty()) {
            allChats
        } else {
            allChats.filter { chat ->
                val user = allUsers.find { it.uid == chat.first }
                val username = user?.username ?: ""
                val email = user?.email ?: ""
                username.contains(query, ignoreCase = true) || email.contains(query, ignoreCase = true)
            }
        }
        chatsAdapter.submitList(filteredChats)
        binding.chatsRecyclerView.scrollToPosition(0)
    }
}