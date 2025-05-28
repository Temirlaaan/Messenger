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

class FragmentSettings : Fragment(R.layout.fragment_settings) {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var userRepository: UserRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel = ViewModelProvider(requireActivity()).get(AuthViewModel::class.java)
        userRepository = UserRepository()

        val usernameText = view.findViewById<TextView>(R.id.usernameText)
        val emailText = view.findViewById<TextView>(R.id.emailText)
        val logoutButton = view.findViewById<Button>(R.id.logoutButton)

        userRepository.getUser(authViewModel.getCurrentUserId()!!) { user ->
            usernameText.text = user.username // Используем username
            emailText.text = user.email
        }

        logoutButton.setOnClickListener {
            authViewModel.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }
}