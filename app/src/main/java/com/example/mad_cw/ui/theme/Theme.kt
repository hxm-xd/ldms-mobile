package com.example.mad_cw.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightPalette: Colors = lightColors(
    primary = FernGreen,
    primaryVariant = HunterGreen,
    secondary = Sage,
    secondaryVariant = BrunswickGreen,
    background = Timberwolf,
    surface = Color.White,
    error = Color(0xFFB00020),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111),
)

private val DarkPalette: Colors = darkColors(
    primary = Sage,
    primaryVariant = FernGreen,
    secondary = Timberwolf,
    secondaryVariant = Timberwolf,
    background = BrunswickGreen,
    surface = HunterGreen,
    error = Color(0xFFCF6679),
    onPrimary = Color(0xFF111111),
    onSecondary = Color(0xFF111111),
    onBackground = Color(0xFFECECEC),
    onSurface = Color(0xFFECECEC),
)

@Composable
fun LDMSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkPalette else LightPalette
    MaterialTheme(
        colors = colors,
        content = content
    )
}
