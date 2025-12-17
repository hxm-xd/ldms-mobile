package com.example.mad_cw.ui.compose

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mad_cw.R
import com.example.mad_cw.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser

@Composable
fun ProfileScreen(authRepository: AuthRepository, onNavigateToDashboard: () -> Unit) {
    val context = LocalContext.current
    var user by remember { mutableStateOf<FirebaseUser?>(authRepository.getCurrentUser()) }
    var displayName by remember { mutableStateOf(user?.displayName ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "Not logged in") }
    var saving by remember { mutableStateOf(false) }
    var changingPassword by remember { mutableStateOf(false) }
    var lastPasswordEmailMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        user = authRepository.getCurrentUser()
        displayName = user?.displayName ?: ""
        email = user?.email ?: "Not logged in"
    }

    Scaffold(topBar = {
        TopAppBar(
            modifier = Modifier.statusBarsPadding(),
            title = { Text("Profile", color = MaterialTheme.colors.onPrimary) },
            navigationIcon = {
                androidx.compose.material.IconButton(onClick = onNavigateToDashboard) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colors.onPrimary)
                }
            },
            backgroundColor = MaterialTheme.colors.primary,
            elevation = 6.dp
        )
    }) { padding ->
        Surface(modifier = Modifier
            .fillMaxSize()
            .padding(padding), color = MaterialTheme.colors.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Card(modifier = Modifier.fillMaxWidth(), elevation = 6.dp, shape = MaterialTheme.shapes.medium) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                        // Use a supported drawable resource for Compose painterResource
                        runCatching { painterResource(id = R.drawable.ic_landslide) }
                            .onSuccess { painter ->
                                Image(painter = painter, contentDescription = null, modifier = Modifier.size(72.dp))
                            }
                            .onFailure {
                                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(72.dp))
                            }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = displayName.ifEmpty { "Anonymous" },
                                style = MaterialTheme.typography.h6
                            )
                            Text(text = email, style = MaterialTheme.typography.body2)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = {
                    saving = true
                    val userObj = authRepository.getCurrentUser()
                    if (userObj != null) {
                        val request = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName.ifEmpty { null })
                            .build()
                        userObj.updateProfile(request).addOnCompleteListener { task ->
                            saving = false
                            if (task.isSuccessful) {
                                user = authRepository.getCurrentUser()
                            }
                        }
                    } else {
                        saving = false
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = if (saving) "Saving..." else "Save")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Change password: send reset email to the user's address
                Button(onClick = {
                    val targetEmail = user?.email
                    if (!targetEmail.isNullOrBlank()) {
                        changingPassword = true
                        com.google.firebase.auth.FirebaseAuth.getInstance()
                            .sendPasswordResetEmail(targetEmail)
                            .addOnCompleteListener { task ->
                                changingPassword = false
                                lastPasswordEmailMessage = if (task.isSuccessful) {
                                    "Password reset email sent to $targetEmail"
                                } else {
                                    task.exception?.localizedMessage ?: "Failed to send reset email"
                                }
                            }
                    } else {
                        lastPasswordEmailMessage = "No email associated with this account"
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = if (changingPassword) "Sending reset email..." else "Change password")
                }

                lastPasswordEmailMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = msg, style = MaterialTheme.typography.caption)
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(onClick = {
                    androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Logout") { _, _ ->
                            authRepository.logoutUser()
                            val intent = Intent(
                                context,
                                com.example.mad_cw.ui.auth.LoginActivity::class.java
                            )
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Logout")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigate to settings to control notifications and other options
                Button(onClick = {
                    val intent = Intent(context, com.example.mad_cw.ui.settings.SettingsActivity::class.java)
                    context.startActivity(intent)
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Settings & notifications")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = onNavigateToDashboard, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Back to Dashboard")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = com.example.mad_cw.R.mipmap.ic_launcher_round),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(text = "Alice Example", style = MaterialTheme.typography.h6)
                    Text(text = "alice@example.com", style = MaterialTheme.typography.body2)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Display name")
            BasicTextField(
                value = "Alice Example", onValueChange = {}, modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = { /* no-op preview */ }) {
                Text(text = "Save")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(onClick = { /* back */ }, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Back to Dashboard")
            }
        }
    }
}
