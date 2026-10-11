package com.nclicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NClickerColors = darkColorScheme(
    primary = Color(0xFFB7C6FF),
    onPrimary = Color(0xFF172448),
    secondary = Color(0xFF75E2D0),
    onSecondary = Color(0xFF003731),
    tertiary = Color(0xFFD8A6FF),
    background = Color(0xFF0B0E15),
    onBackground = Color(0xFFE4E7F2),
    surface = Color(0xFF121722),
    onSurface = Color(0xFFE4E7F2),
    surfaceVariant = Color(0xFF202636),
    onSurfaceVariant = Color(0xFFB9C0D4),
    outline = Color(0xFF41495E),
    error = Color(0xFFFFB4AB)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NClickerTheme {
                NClickerApp()
            }
        }
    }
}

@Composable
private fun NClickerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NClickerColors, content = content)
}
