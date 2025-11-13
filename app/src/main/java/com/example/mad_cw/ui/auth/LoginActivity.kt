package com.example.mad_cw.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.example.mad_cw.ui.compose.LoginScreen
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.example.mad_cw.R
import com.example.mad_cw.data.repository.AuthRepository
import com.example.mad_cw.viewmodel.AuthViewModel
import com.example.mad_cw.ui.auth.SignUpActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnSignUp: Button
    
    private val authViewModel: AuthViewModel by viewModels()
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Use Compose content
        setContent {
            LoginScreen(authViewModel = authViewModel,
                onNavigateToDashboard = {
                    startActivity(Intent(this, com.example.mad_cw.ui.dashboard.DashboardActivity::class.java))
                    finish()
                },
                onNavigateToSignUp = {
                    startActivity(Intent(this, SignUpActivity::class.java))
                }
            )
        }
        
        // Check if user is already logged in (after everything is initialized)
        // Use post to ensure it runs after the layout is fully loaded
        window.decorView.post {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    android.util.Log.d("LoginActivity", "User already logged in, redirecting to Dashboard")
                    startActivity(Intent(this, com.example.mad_cw.ui.dashboard.DashboardActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                android.util.Log.e("LoginActivity", "Error checking auth state: ${e.message}", e)
            }
        }
    }
    
    private fun initViews() {
        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnSignUp = findViewById(R.id.btnSignUpRedirect)
    }
    
    private fun setupObservers() {
        authViewModel.loginResult.observe(this, Observer { success ->
            if (success) {
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, com.example.mad_cw.ui.dashboard.DashboardActivity::class.java))
                finish()
            }
        })
        
        authViewModel.errorMessage.observe(this, Observer { error ->
            error?.let {
                Toast.makeText(this, "Error: $it", Toast.LENGTH_SHORT).show()
            }
        })
        
        authViewModel.registerResult.observe(this, Observer { success ->
            if (success) {
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, com.example.mad_cw.ui.dashboard.DashboardActivity::class.java))
                finish()
            }
        })
    }
    
    private fun setupClickListeners() {
        // Click handlers moved into Compose LoginScreen
    }
}