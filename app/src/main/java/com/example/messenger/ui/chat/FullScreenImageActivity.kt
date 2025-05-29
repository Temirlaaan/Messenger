package com.example.messenger.ui.chat

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.example.messenger.databinding.ActivityFullscreenImageBinding

class FullScreenImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullscreenImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullscreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Скрываем ActionBar
        supportActionBar?.hide()

        // Современный способ скрытия системных панелей
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        val imageUrl = intent.getStringExtra("imageUrl")

        imageUrl?.let {
            Glide.with(this)
                .load(it)
                .into(binding.fullscreenImageView)
        }

        // Закрытие по клику на изображение
        binding.fullscreenImageView.setOnClickListener {
            finish()
        }

        // Кнопка закрытия
        binding.closeButton.setOnClickListener {
            finish()
        }
    }
}