package com.example.messenger.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.messenger.R
import com.example.messenger.ui.auth.login.LoginActivity
import com.example.messenger.viewmodel.AuthViewModel
import com.example.messenger.data.repository.UserRepository
import com.example.messenger.databinding.FragmentSettingsBinding
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.util.*

class FragmentSettings : Fragment(R.layout.fragment_settings) {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var userRepository: UserRepository
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val storage = FirebaseStorage.getInstance()

    // Лаунчер для выбора изображения
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            uploadProfileImage(it)
        }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel = ViewModelProvider(requireActivity()).get(AuthViewModel::class.java)
        userRepository = UserRepository()

        // Показываем ProgressBar во время загрузки
        binding.loadingProgressBar.visibility = View.VISIBLE
        binding.profileImageView.visibility = View.GONE
        binding.usernameInputLayout.visibility = View.GONE
        binding.emailCard.visibility = View.GONE
        binding.saveButton.visibility = View.GONE
        binding.logoutButton.visibility = View.GONE

        loadUserData()

        // Обработчик клика по изображению профиля
        binding.profileImageView.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Обработчик кнопки "Сохранить"
        binding.saveButton.setOnClickListener {
            saveUserData()
        }

        binding.logoutButton.setOnClickListener {
            authViewModel.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadUserData() {
        val currentUserId = authViewModel.getCurrentUserId()
        if (currentUserId != null && isAdded) {
            lifecycleScope.launch {
                userRepository.getUser(currentUserId) { user ->
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.profileImageView.visibility = View.VISIBLE
                    binding.usernameInputLayout.visibility = View.VISIBLE
                    binding.emailCard.visibility = View.VISIBLE
                    binding.saveButton.visibility = View.VISIBLE
                    binding.logoutButton.visibility = View.VISIBLE

                    // Заполняем поля
                    binding.usernameEditText.setText(user.username)
                    binding.emailTextView.text = user.email

                    // Загружаем аватар пользователя
                    if (!user.profileImageUrl.isNullOrEmpty() && isAdded) {
                        Glide.with(requireContext())
                            .load(user.profileImageUrl)
                            .placeholder(R.drawable.ic_profile_picture)
                            .error(R.drawable.ic_profile_picture)
                            .circleCrop()
                            .into(binding.profileImageView)
                    } else if (isAdded) {
                        binding.profileImageView.setImageResource(R.drawable.ic_profile_picture)
                    }
                }
            }
        }
    }

    private fun saveUserData() {
        val currentUserId = authViewModel.getCurrentUserId() ?: return
        val newUsername = binding.usernameEditText.text.toString().trim()

        // Валидация имени пользователя
        if (newUsername.isEmpty()) {
            binding.usernameInputLayout.error = "Имя пользователя не может быть пустым"
            return
        } else if (newUsername.length < 3) {
            binding.usernameInputLayout.error = "Имя пользователя должно быть минимум 3 символа"
            return
        } else {
            binding.usernameInputLayout.error = null
        }

        binding.loadingProgressBar.visibility = View.VISIBLE
        binding.saveButton.isEnabled = false

        // Проверяем уникальность имени пользователя
        userRepository.getAllUsers(exceptUserId = currentUserId) { users ->
            if (users.any { it.username == newUsername && it.uid != currentUserId }) {
                binding.usernameInputLayout.error = "Это имя пользователя уже занято"
                binding.loadingProgressBar.visibility = View.GONE
                binding.saveButton.isEnabled = true
                return@getAllUsers
            }
            // Если имя уникально, продолжаем сохранение
            val userMap = mapOf("username" to newUsername)
            userRepository.saveUser(currentUserId, userMap)
            Toast.makeText(requireContext(), "Имя пользователя обновлено", Toast.LENGTH_SHORT).show()
            binding.loadingProgressBar.visibility = View.GONE
            binding.saveButton.isEnabled = true
        }
    }

    private fun uploadProfileImage(imageUri: Uri) {
        val currentUserId = authViewModel.getCurrentUserId() ?: return

        binding.loadingProgressBar.visibility = View.VISIBLE

        val storageRef = storage.reference
        val imageRef = storageRef.child("profile_images/${currentUserId}_${UUID.randomUUID()}.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val userMap = mapOf("profileImageUrl" to downloadUri.toString())
                    userRepository.saveUser(currentUserId, userMap)

                    if (isAdded) {
                        Glide.with(requireContext())
                            .load(downloadUri)
                            .placeholder(R.drawable.ic_profile_picture)
                            .error(R.drawable.ic_profile_picture)
                            .circleCrop()
                            .into(binding.profileImageView)
                    }

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