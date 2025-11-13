package com.example.mad_cw.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.mad_cw.R
import com.example.mad_cw.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ProfileScreen(authRepository: AuthRepository, onNavigateToDashboard: () -> Unit) {
    val context = LocalContext.current
    var user by remember { mutableStateOf<FirebaseUser?>(authRepository.getCurrentUser()) }
    var displayName by remember { mutableStateOf(user?.displayName ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "Not logged in") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        user = authRepository.getCurrentUser()
        displayName = user?.displayName ?: ""
        email = user?.email ?: "Not logged in"
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(id = R.mipmap.ic_launcher_round), contentDescription = null,
                    modifier = Modifier.size(96.dp))

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(text = displayName.ifEmpty { "Anonymous" }, style = MaterialTheme.typography.h6)
                    Text(text = email, style = MaterialTheme.typography.body2)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Display name")
            BasicTextField(value = displayName, onValueChange = { displayName = it }, modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp))

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = {
                saving = true
                // update profile asynchronously
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
            }) {
                Text(text = if (saving) "Saving..." else "Save")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(onClick = onNavigateToDashboard, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Back to Dashboard")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(id = com.example.mad_cw.R.mipmap.ic_launcher_round), contentDescription = null,
                    modifier = Modifier.size(96.dp))

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(text = "Alice Example", style = MaterialTheme.typography.h6)
                    Text(text = "alice@example.com", style = MaterialTheme.typography.body2)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Display name")
            BasicTextField(value = "Alice Example", onValueChange = {}, modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp))

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
