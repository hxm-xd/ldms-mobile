package com.example.mad_cw.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import com.example.mad_cw.ui.theme.LDMSTheme
import com.example.mad_cw.data.repository.AuthRepository
import com.example.mad_cw.ui.compose.ProfileScreen

class ProfileActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LDMSTheme {
                ProfileScreen(authRepository = authRepository, onNavigateToDashboard = {
                    startActivity(
                        Intent(
                            this,
                            com.example.mad_cw.ui.dashboard.DashboardActivity::class.java
                        )
                    )
                    finish()
                })
            }
        }
    }
}
