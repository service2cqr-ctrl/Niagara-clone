package com.niagaraclone.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppTheme { LIGHT, DARK, AUTO, SYSTEM }

@Composable
fun NiagaraCloneTheme(
    appTheme: AppTheme = AppTheme.AUTO,
    accentColor: Color = Color(0xFF4DD0E1),
    content: @Composable () -> Unit
) {
    val useDark = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.AUTO, AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = accentColor,
            background = Color.Black, // True AMOLED Pitch Black matching Niagara
            surface = Color(0xFF141414), // Rich dark surface for cards and sheets
            onBackground = Color(0xFFECECEC),
            onSurface = Color(0xFFECECEC)
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            background = Color(0xFFFAFAFA),
            surface = Color.White,
            onBackground = Color(0xFF1A1A1A),
            onSurface = Color(0xFF1A1A1A)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

/** Preset accent colors offered in the theme picker, echoing Niagara's palette options. */
val AccentPresets = listOf(
    Color(0xFF4DD0E1), // Teal (Default)
    Color(0xFF3B82F6), // Blue
    Color(0xFF10B981), // Green
    Color(0xFFF59E0B), // Amber
    Color(0xFFEF4444), // Red
    Color(0xFF8B5CF6), // Violet
    Color(0xFFEC4899), // Pink
    Color(0xFF64748B)  // Slate
)
