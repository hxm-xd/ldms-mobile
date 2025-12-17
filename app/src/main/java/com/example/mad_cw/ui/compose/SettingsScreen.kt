package com.example.mad_cw.ui.compose

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.appcompat.app.AppCompatDelegate

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("LDMS_PREFS", Context.MODE_PRIVATE) }
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", true)) }
    var darkModeEnabled by remember { mutableStateOf(prefs.getBoolean("dark_mode_enabled", false)) }

    fun updateNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun updateDarkMode(enabled: Boolean) {
        darkModeEnabled = enabled
        prefs.edit().putBoolean("dark_mode_enabled", enabled).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Settings", color = MaterialTheme.colors.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colors.onPrimary)
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                elevation = 6.dp
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Notifications", style = MaterialTheme.typography.h6)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colors.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("High-threat nearby alerts", style = MaterialTheme.typography.body1)
                            Text(
                                "Notify when a sensor within 1km reaches HIGH threat level.",
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }
                    Switch(checked = notificationsEnabled, onCheckedChange = { updateNotifications(it) })
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Appearance", style = MaterialTheme.typography.h6)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Brightness4, contentDescription = null, tint = MaterialTheme.colors.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Dark mode", style = MaterialTheme.typography.body1)
                            Text("Use a darker color scheme.", style = MaterialTheme.typography.caption)
                        }
                    }
                    Switch(checked = darkModeEnabled, onCheckedChange = { updateDarkMode(it) })
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    Surface(modifier = Modifier.fillMaxSize()) {
        SettingsScreen(onBack = { })
    }
}
