package com.example.messenger.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.messenger.MainActivity
import com.example.messenger.R
import com.example.messenger.data.repository.AuthRepository
import com.example.messenger.ui.auth.confirm.ConfirmActivity
import com.example.messenger.ui.auth.register.RegisterActivity
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authRepository = AuthRepository()

        val emailEditText = findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordEditText = findViewById<TextInputEditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val tvRegister = findViewById<TextView>(R.id.tv_register)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Введите email и пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            loginButton.isEnabled = false

            authRepository.signIn(email, password)
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE
                    loginButton.isEnabled = true

                    if (task.isSuccessful) {
                        // Перезагружаем данные пользователя для актуального статуса верификации
                        authRepository.reloadUser()?.addOnCompleteListener { reloadTask ->
                            if (authRepository.isEmailVerified()) {
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "Необходимо подтвердить email", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, ConfirmActivity::class.java).apply {
                                    putExtra("email", email)
                                })
                            }
                        } ?: run {
                            // Если reloadUser вернул null, проверяем текущий статус
                            if (authRepository.isEmailVerified()) {
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "Необходимо подтвердить email", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, ConfirmActivity::class.java).apply {
                                    putExtra("email", email)
                                })
                            }
                        }
                    } else {
                        val errorMessage = when {
                            task.exception?.message?.contains("user not found") == true ->
                                "Пользователь не найден"
                            task.exception?.message?.contains("wrong password") == true ->
                                "Неверный пароль"
                            task.exception?.message?.contains("invalid email") == true ->
                                "Неверный формат email"
                            else -> "Ошибка входа: ${task.exception?.message}"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}