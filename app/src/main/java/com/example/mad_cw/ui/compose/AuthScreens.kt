package com.example.mad_cw.ui.compose

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.livedata.observeAsState
import com.example.mad_cw.R
import com.example.mad_cw.data.repository.AuthRepository
import com.example.mad_cw.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToDashboard: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginResult by authViewModel.loginResult.observeAsState()
    val errorMessage by authViewModel.errorMessage.observeAsState()

    LaunchedEffect(loginResult) {
        if (loginResult == true) {
            onNavigateToDashboard()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Image(painter = painterResource(id = R.drawable.ic_landslide), contentDescription = null, modifier = Modifier.size(140.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "LDMS - Alerts App", style = MaterialTheme.typography.h5, color = MaterialTheme.colors.primary)
            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), elevation = 6.dp) {
                Column(modifier = Modifier.padding(24.dp)) {
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = {
                        authViewModel.login(email.trim(), password.trim())
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Login")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { onNavigateToSignUp() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Sign Up")
                    }
                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = msg, color = MaterialTheme.colors.error)
                    }
                }
            }
        }
    }
}

@Composable
fun SignUpScreen(authViewModel: AuthViewModel = viewModel(), onNavigateToLogin: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val registerResult by authViewModel.registerResult.observeAsState()
    val errorMessage by authViewModel.errorMessage.observeAsState()

    LaunchedEffect(registerResult) {
        if (registerResult == true) {
            onNavigateToLogin()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(48.dp))
            Image(painter = painterResource(id = R.drawable.ic_landslide), contentDescription = null, modifier = Modifier.size(140.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Create Account", style = MaterialTheme.typography.h5, color = MaterialTheme.colors.primary)

            Card(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), elevation = 6.dp) {
                Column(modifier = Modifier.padding(24.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("Confirm Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = {
                        if (email.isBlank() || password.isBlank() || confirm.isBlank()) return@Button
                        if (password != confirm) return@Button
                        authViewModel.register(email.trim(), password.trim())
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Sign Up")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { onNavigateToLogin() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Already have an account? Login")
                    }
                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = msg, color = MaterialTheme.colors.error)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("LDMS_PREFS", Context.MODE_PRIVATE)
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", true)) }
    var darkModeEnabled by remember { mutableStateOf(prefs.getBoolean("dark_mode_enabled", false)) }
    val authRepo = AuthRepository()
    val user = authRepo.getCurrentUser()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                Text(text = "Settings", style = MaterialTheme.typography.h6)
            }

            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), elevation = 4.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Account Information", style = MaterialTheme.typography.subtitle1)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Email")
                    Text(text = user?.email ?: "Not logged in")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "User ID")
                    Text(text = user?.uid ?: "N/A")
                }
            }

            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), elevation = 4.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Preferences", style = MaterialTheme.typography.subtitle1)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Push Notifications")
                            Text(text = "Receive alerts for high-risk sensors", style = MaterialTheme.typography.caption)
                        }
                        Switch(checked = notificationsEnabled, onCheckedChange = { checked ->
                            notificationsEnabled = checked
                            prefs.edit().putBoolean("notifications_enabled", checked).apply()
                        })
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Dark Mode")
                            Text(text = "Switch to dark theme", style = MaterialTheme.typography.caption)
                        }
                        Switch(checked = darkModeEnabled, onCheckedChange = { checked ->
                            darkModeEnabled = checked
                            prefs.edit().putBoolean("dark_mode_enabled", checked).apply()
                            if (checked) androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES) else androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = {
                androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout") { _, _ ->
                        authRepo.logoutUser()
                        val intent = Intent(context, com.example.mad_cw.ui.auth.LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Logout")
            }
        }
    }
}

@Composable
fun AuthPreviews() {}
