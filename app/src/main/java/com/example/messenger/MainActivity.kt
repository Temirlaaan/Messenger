package com.example.messenger

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.messenger.ui.auth.login.LoginActivity
import com.example.messenger.viewmodel.AuthViewModel
import com.example.messenger.viewmodel.ViewModelStoreOwnerProvider
import com.example.messenger.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        // Устанавливаем ViewModelStoreOwner для доступа к ViewModel
        ViewModelStoreOwnerProvider.setViewModelStoreOwner(this)

        if (authViewModel.getCurrentUserId() == null || !authViewModel.isEmailVerified()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
    }
}