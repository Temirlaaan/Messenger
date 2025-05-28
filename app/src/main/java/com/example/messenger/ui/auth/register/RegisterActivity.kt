package com.example.messenger.ui.auth.register

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.messenger.R
import com.example.messenger.data.repository.AuthRepository
import com.example.messenger.data.repository.UserRepository
import com.example.messenger.ui.auth.confirm.ConfirmActivity
import com.example.messenger.ui.auth.login.LoginActivity
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        authRepository = AuthRepository()

        val emailEditText = findViewById<TextInputEditText>(R.id.rgemailEditText)
        val usernameEditText = findViewById<TextInputEditText>(R.id.usernameEditText) // Новый EditText
        val passwordEditText = findViewById<TextInputEditText>(R.id.rgpasswordEditText)
        val repeatPasswordEditText = findViewById<TextInputEditText>(R.id.repeatPasswordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val tvLogin = findViewById<TextView>(R.id.tv_login)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim() // Получаем username
            val password = passwordEditText.text.toString().trim()
            val repeatPassword = repeatPasswordEditText.text.toString().trim()

            if (email.isEmpty() || username.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != repeatPassword) {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authRepository.register(email, password) { success, error ->
                if (success) {
                    val userMap = mapOf(
                        "username" to username, // Сохраняем введенный username
                        "email" to email
                    )
                    val userRepository = UserRepository()
                    userRepository.saveUser(authRepository.getCurrentUserId()!!, userMap)
                    startActivity(Intent(this, ConfirmActivity::class.java).apply {
                        putExtra("email", email) // Передаем email для подтверждения
                    })
                    finish()
                } else {
                    Toast.makeText(this, "Ошибка: $error", Toast.LENGTH_SHORT).show()
                }
            }
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}