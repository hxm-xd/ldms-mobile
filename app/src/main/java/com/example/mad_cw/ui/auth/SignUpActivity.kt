package com.example.mad_cw.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_cw.R
import com.example.mad_cw.data.repository.AuthRepository

class SignUpActivity : AppCompatActivity() {

    private lateinit var inputName: EditText
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var inputConfirmPassword: EditText
    private lateinit var btnSignUp: Button
    private lateinit var btnLoginRedirect: Button
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        android.os.StrictMode.setThreadPolicy(
            android.os.StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )

        setContentView(R.layout.activity_signup)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        inputName = findViewById(R.id.inputName)
        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword)
        btnSignUp = findViewById(R.id.btnSignUpRedirect)
        btnLoginRedirect = findViewById(R.id.btnLoginRedirect)
    }

    private fun setupClickListeners() {
        btnSignUp.setOnClickListener {
            try {
                val email = inputEmail.text.toString().trim()
                val password = inputPassword.text.toString().trim()
                val confirmPassword = inputConfirmPassword.text.toString().trim()

                if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (password != confirmPassword) {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (password.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                authRepository.registerUser(email, password) { success, error ->
                    if (success) {
                        Toast.makeText(this, "Sign-up successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        btnLoginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
