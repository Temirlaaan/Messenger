package com.example.messenger.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.messenger.R
import com.example.messenger.ui.auth.login.LoginActivity
import com.example.messenger.viewmodel.AuthViewModel
import com.example.messenger.data.repository.UserRepository
import com.example.messenger.databinding.FragmentSettingsBinding
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class FragmentSettings : Fragment(R.layout.fragment_settings) {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var userRepository: UserRepository
    private lateinit var binding: FragmentSettingsBinding
    private val storage = FirebaseStorage.getInstance()

    // Лаунчер для выбора изображения
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            uploadProfileImage(it)
        }
    }

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

        loadUserData()

        // Обработчик клика по изображению профиля
        binding.profileImageView.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.logoutButton.setOnClickListener {
            authViewModel.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun loadUserData() {
        val currentUserId = authViewModel.getCurrentUserId()
        if (currentUserId != null) {
            userRepository.getUser(currentUserId) { user ->
                // Скрываем ProgressBar и показываем данные
                binding.loadingProgressBar.visibility = View.GONE
                binding.profileImageView.visibility = View.VISIBLE
                binding.usernameText.visibility = View.VISIBLE
                binding.emailText.visibility = View.VISIBLE
                binding.logoutButton.visibility = View.VISIBLE

                binding.usernameText.text = user.username
                binding.emailText.text = user.email

                // Загружаем аватар пользователя
                if (user.profileImageUrl?.isNotEmpty() == true) {
                    Glide.with(this)
                        .load(user.profileImageUrl)
                        .placeholder(R.drawable.ic_profile_picture)
                        .error(R.drawable.ic_profile_picture)
                        .circleCrop()
                        .into(binding.profileImageView)
                } else {
                    binding.profileImageView.setImageResource(R.drawable.ic_profile_picture)
                }
            }
        }
    }

    private fun uploadProfileImage(imageUri: Uri) {
        val currentUserId = authViewModel.getCurrentUserId() ?: return

        // Показываем индикатор загрузки
        binding.loadingProgressBar.visibility = View.VISIBLE

        val storageRef = storage.reference
        val imageRef = storageRef.child("profile_images/${currentUserId}_${UUID.randomUUID()}.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                // Получаем URL загруженного изображения
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Сохраняем URL в базе данных
                    val userMap = mapOf("profileImageUrl" to downloadUri.toString())
                    userRepository.saveUser(currentUserId, userMap)

                    // Обновляем UI
                    Glide.with(this)
                        .load(downloadUri)
                        .placeholder(R.drawable.ic_profile_picture)
                        .error(R.drawable.ic_profile_picture)
                        .circleCrop()
                        .into(binding.profileImageView)

                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Фото профиля обновлено", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                binding.loadingProgressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Ошибка загрузки изображения: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
}