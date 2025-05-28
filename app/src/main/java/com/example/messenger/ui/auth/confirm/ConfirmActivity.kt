package com.example.messenger.ui.auth.confirm

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.messenger.MainActivity
import com.example.messenger.R
import com.google.android.material.textfield.TextInputEditText

class ConfirmActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.confirm_activity)

        val email = intent.getStringExtra("email") ?: ""
        val codeEditText = findViewById<TextInputEditText>(R.id.codeEditText)
        val confirmButton = findViewById<Button>(R.id.confirmButton)

        // Для теста генерируем код (в реальном приложении отправь через email)
        val code = (100000..999999).random().toString()
        Toast.makeText(this, "Код для $email: $code", Toast.LENGTH_LONG).show() // Замени на отправку

        confirmButton.setOnClickListener {
            val inputCode = codeEditText.text.toString().trim()
            if (inputCode == code) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Неверный код", Toast.LENGTH_SHORT).show()
            }
        }
    }
}