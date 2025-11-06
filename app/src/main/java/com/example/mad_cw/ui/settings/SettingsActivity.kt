package com.example.mad_cw.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.mad_cw.R
import com.example.mad_cw.data.repository.AuthRepository
import com.example.mad_cw.ui.auth.LoginActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvEmail: TextView
    private lateinit var tvUserId: TextView
    private lateinit var switchNotifications: Switch
    private lateinit var switchDarkMode: Switch
    private lateinit var prefs: SharedPreferences
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        prefs = getSharedPreferences("LDMS_PREFS", MODE_PRIVATE)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
        
        initViews()
        loadSettings()
        setupClickListeners()
    }
    
    private fun initViews() {
        tvEmail = findViewById(R.id.tvEmail)
        tvUserId = findViewById(R.id.tvUserId)
        switchNotifications = findViewById(R.id.switchNotifications)
        switchDarkMode = findViewById(R.id.switchDarkMode)
    }
    
    private fun loadSettings() {
        val user = authRepository.getCurrentUser()
        tvEmail.text = user?.email ?: "Not logged in"
        tvUserId.text = user?.uid ?: "N/A"
        
        // Load notification preference
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        switchNotifications.isChecked = notificationsEnabled
        
        // Load dark mode preference
        val darkModeEnabled = prefs.getBoolean("dark_mode_enabled", false)
        switchDarkMode.isChecked = darkModeEnabled
        applyDarkMode(darkModeEnabled)
    }
    
    private fun setupClickListeners() {
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
            if (isChecked) {
                // Enable notifications
                // You can integrate with FCM here
            } else {
                // Disable notifications
            }
        }
        
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode_enabled", isChecked).apply()
            applyDarkMode(isChecked)
        }
        
        findViewById<TextView>(R.id.btnLogout).setOnClickListener {
            showLogoutDialog()
        }
    }
    
    private fun applyDarkMode(enabled: Boolean) {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                authRepository.logoutUser()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            finish()
        } else {
            @Suppress("DEPRECATION")
            onBackPressed()
        }
        return true
    }
}

