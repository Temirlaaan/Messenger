package com.example.messenger.ui.auth.register

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.messenger.R
import com.example.messenger.data.repository.AuthRepository
import com.example.messenger.data.repository.UserRepository
import com.example.messenger.ui.auth.confirm.ConfirmActivity
import com.example.messenger.ui.auth.login.LoginActivity
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.AuthResult

class RegisterActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        authRepository = AuthRepository()

        val emailEditText = findViewById<TextInputEditText>(R.id.rgemailEditText)
        val usernameEditText = findViewById<TextInputEditText>(R.id.usernameEditText)
        val passwordEditText = findViewById<TextInputEditText>(R.id.rgpasswordEditText)
        val repeatPasswordEditText = findViewById<TextInputEditText>(R.id.repeatPasswordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val tvLogin = findViewById<TextView>(R.id.tv_login)

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()
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

            progressBar.visibility = View.VISIBLE
            registerButton.isEnabled = false
            authRepository.register(email, password)
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE
                    registerButton.isEnabled = true
                    if (task.isSuccessful) {
                        val userRepository = UserRepository()
                        val userId = authRepository.getCurrentUserId()
                        if (userId != null) {
                            val userMap = mapOf(
                                "username" to username,
                                "email" to email
                            )
                            userRepository.saveUser(userId, userMap)
                            startActivity(Intent(this, ConfirmActivity::class.java).apply {
                                putExtra("email", email)
                            })
                            finish()
                        } else {
                            Toast.makeText(this, "Ошибка: не удалось получить ID пользователя", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Ошибка: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}