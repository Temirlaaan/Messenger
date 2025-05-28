package com.example.messenger.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.messenger.R
import com.example.messenger.ui.auth.login.LoginActivity
import com.example.messenger.viewmodel.AuthViewModel
import com.example.messenger.data.repository.UserRepository
import com.example.messenger.databinding.FragmentSettingsBinding

class FragmentSettings : Fragment(R.layout.fragment_settings) {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var userRepository: UserRepository
    private lateinit var binding: FragmentSettingsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSettingsBinding.bind(view)
        authViewModel = ViewModelProvider(requireActivity()).get(AuthViewModel::class.java)
        userRepository = UserRepository()

        // Показываем ProgressBar во время загрузки
        binding.loadingProgressBar.visibility = View.VISIBLE
        binding.profileImageView.visibility = View.GONE
        binding.usernameText.visibility = View.GONE
        binding.emailText.visibility = View.GONE
        binding.logoutButton.visibility = View.GONE

        userRepository.getUser(authViewModel.getCurrentUserId()!!) { user ->
            // Скрываем ProgressBar и показываем данные
            binding.loadingProgressBar.visibility = View.GONE
            binding.profileImageView.visibility = View.VISIBLE
            binding.usernameText.visibility = View.VISIBLE
            binding.emailText.visibility = View.VISIBLE
            binding.logoutButton.visibility = View.VISIBLE

            binding.usernameText.text = user.username // Используем username
            binding.emailText.text = user.email
        }

        binding.logoutButton.setOnClickListener {
            authViewModel.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }
}