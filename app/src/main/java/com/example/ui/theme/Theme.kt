package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val CyberDarkColorScheme = darkColorScheme(
  primary = NeonRed,
  secondary = NeonCyan,
  tertiary = NeonPurple,
  background = CyberBlack,
  surface = CyberCard,
  onPrimary = TextPrimary,
  onSecondary = CyberBlack,
  onTertiary = TextPrimary,
  onBackground = TextPrimary,
  onSurface = TextPrimary,
  surfaceVariant = CyberCard,
  onSurfaceVariant = TextSecondary,
  outline = CyberCardBorder
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme by default for gaming layout
  dynamicColor: Boolean = false, // Disable dynamic colors to keep intense Armoury Crate visual look branding
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) CyberDarkColorScheme else CyberDarkColorScheme // Force dark for optimal game style

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
