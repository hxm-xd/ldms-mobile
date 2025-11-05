package com.example.mad_cw.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
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
        setContentView(R.layout.activity_login)
        
        val rootView = findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        initViews()
        setupObservers()
        setupClickListeners()
        
        // Check if user is already logged in (after everything is initialized)
        // Use post to ensure it runs after the layout is fully loaded
        rootView.post {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    android.util.Log.d("LoginActivity", "User already logged in, redirecting to Dashboard")
                    startActivity(Intent(this, com.example.mad_cw.ui.dashboard.DashboardActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                android.util.Log.e("LoginActivity", "Error checking auth state: ${e.message}", e)
                // Continue to show login screen if there's an error
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
        btnLogin.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            authViewModel.login(email, password)
        }
        
        btnSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}