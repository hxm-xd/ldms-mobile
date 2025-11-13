package com.example.mad_cw.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mad_cw.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

    private val _registerResult = MutableLiveData<Boolean>()
    val registerResult: LiveData<Boolean> = _registerResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    fun login(email: String, password: String) {
        repository.loginUser(email, password) { success, error ->
            _loginResult.value = success
            _errorMessage.value = error
            if (success) {
                _currentUser.value = repository.getCurrentUser()
            }
        }
    }

    fun register(email: String, password: String) {
        repository.registerUser(email, password) { success, error ->
            _registerResult.value = success
            _errorMessage.value = error
            if (success) {
                _currentUser.value = repository.getCurrentUser()
            }
        }
    }

    fun logout() {
        repository.logoutUser()
        _currentUser.value = null
    }

    fun getCurrentUser() {
        _currentUser.value = repository.getCurrentUser()
    }
}

