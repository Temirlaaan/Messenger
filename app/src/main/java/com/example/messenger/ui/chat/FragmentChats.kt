package com.example.messenger.ui.chats

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messenger.R
import com.example.messenger.data.models.Message
import com.example.messenger.ui.chat.ChatActivity
import com.example.messenger.viewmodel.AuthViewModel
import com.example.messenger.viewmodel.ChatsViewModel
import com.example.messenger.viewmodel.ChatsViewModelFactory
import com.example.messenger.databinding.FragmentChatsBinding
import java.text.SimpleDateFormat
import java.util.*

class FragmentChats : Fragment(R.layout.fragment_chats) {

    private lateinit var binding: FragmentChatsBinding
    private lateinit var chatsViewModel: ChatsViewModel
    private lateinit var authViewModel: AuthViewModel
    private lateinit var chatsAdapter: ChatsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentChatsBinding.bind(view)
        authViewModel = ViewModelProvider(requireActivity()).get(AuthViewModel::class.java)
        chatsViewModel = ViewModelProvider(requireActivity(), ChatsViewModelFactory(authViewModel)).get(ChatsViewModel::class.java)

        // Инициализируем адаптер один раз
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

        // Наблюдаем за изменениями состояния
        chatsViewModel.chatsState.observe(viewLifecycleOwner) { state ->
            Log.d("FragmentChats", "Chats state updated: chats=${state.chats.size}, users=${state.users.size}, error=${state.error}, isLoading=${state.isLoading}")
            if (state.isLoading) {
                binding.loadingProgressBar.animate().alpha(1f).setDuration(200).start()
                binding.chatsRecyclerView.animate().alpha(0f).setDuration(200).withEndAction {
                    binding.loadingProgressBar.visibility = View.VISIBLE
                    binding.chatsRecyclerView.visibility = View.GONE
                }.start()
            } else {
                binding.loadingProgressBar.animate().alpha(0f).setDuration(200).withEndAction {
                    binding.loadingProgressBar.visibility = View.GONE
                }.start()
                if (state.chats.isNotEmpty()) {
                    binding.chatsRecyclerView.visibility = View.VISIBLE
                    binding.chatsRecyclerView.animate().alpha(1f).setDuration(200).start()
                    chatsAdapter.updateUsers(state.users)
                    chatsAdapter.submitList(state.chats)
                } else {
                    binding.chatsRecyclerView.animate().alpha(0f).setDuration(200).withEndAction {
                        binding.chatsRecyclerView.visibility = View.GONE
                    }.start()
                }
            }
            if (state.error != null) {
                Toast.makeText(requireContext(), "Ошибка загрузки чатов: ${state.error}", Toast.LENGTH_LONG).show()
            }
            if (state.chats.isEmpty() && !state.isLoading) {
                Toast.makeText(requireContext(), "Чаты не найдены", Toast.LENGTH_SHORT).show()
            }
        }

        // Загружаем данные, если пользователь авторизован
        val userId = authViewModel.getCurrentUserId()
        if (userId != null) {
            Log.d("FragmentChats", "Loading chats for userId=$userId")
            chatsViewModel.loadChats(userId)
        } else {
            Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_LONG).show()
        }
    }
}