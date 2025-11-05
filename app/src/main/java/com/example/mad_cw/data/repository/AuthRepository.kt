package com.example.mad_cw.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    fun loginUser(email: String, password: String, onResult: (Boolean, String?) -> Unit){
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                    task -> if(task.isSuccessful) {
                onResult(true, null)
            } else{
                onResult(false, task.exception?.message)
            }
            }
    }

    fun registerUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("AuthRepository", "User registered successfully: ${auth.currentUser?.uid}")
                    onResult(true, null)
                } else {
                    android.util.Log.e("AuthRepository", "Registration failed: ${task.exception?.message}")
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun logoutUser(){
        auth.signOut()
    }
}