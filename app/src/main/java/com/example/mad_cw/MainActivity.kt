package com.example.mad_cw

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_cw.data.repository.AuthRepository
import com.example.mad_cw.ui.auth.DashboardActivity
import com.example.mad_cw.ui.auth.LoginActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val authRepository = AuthRepository()
        val intent = if (authRepository.getCurrentUser() != null) {
            Intent(this, DashboardActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}