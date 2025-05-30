package com.example.messenger.ui.auth.confirm

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.messenger.MainActivity
import com.example.messenger.R
import com.example.messenger.data.repository.AuthRepository
import com.example.messenger.ui.auth.login.LoginActivity

class ConfirmActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.confirm_activity)

        authRepository = AuthRepository()
        val email = intent.getStringExtra("email") ?: ""

        val emailTextView = findViewById<TextView>(R.id.emailTextView)
        val checkButton = findViewById<Button>(R.id.checkButton)
        val resendButton = findViewById<Button>(R.id.resendButton)
        val backToLoginButton = findViewById<Button>(R.id.backToLoginButton)

        // Отображаем email
        emailTextView.text = "Письмо отправлено на: $email"

        // Проверяем верификацию
        checkButton.setOnClickListener {
            authRepository.reloadUser()?.addOnCompleteListener { reloadTask ->
                if (reloadTask.isSuccessful) {
                    if (authRepository.isEmailVerified()) {
                        Toast.makeText(this, "Email подтвержден!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Email еще не подтвержден. Проверьте почту.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Ошибка проверки статуса", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Повторная отправка письма
        resendButton.setOnClickListener {
            authRepository.sendEmailVerification()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Письмо отправлено повторно", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Ошибка отправки письма", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Возврат к логину
        backToLoginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}