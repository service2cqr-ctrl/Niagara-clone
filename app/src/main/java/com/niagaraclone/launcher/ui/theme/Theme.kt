package com.niagaraclone.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

enum class AppTheme {
    PITCH_BLACK,
    LIGHT,
    GLASS
}

@Immutable
data class LauncherPalette(
    val theme: AppTheme = AppTheme.PITCH_BLACK,
    val accent: Color = Color(0xFF4DD0E1),
    val screenBackground: Color = Color.Black,
    val sheetBackground: Color = Color(0xFF0A0A0A),
    val cardBackground: Color = Color(0xFF18181B),
    val borderColor: Color = Color(0x26FFFFFF),
    val textPrimary: Color = Color.White,
    val textSecondary: Color = Color(0xFFA1A1AA),
    val homeTextColor: Color = Color.White,
    val isLight: Boolean = false,
    val isGlass: Boolean = false
)

val LocalLauncherPalette = staticCompositionLocalOf { LauncherPalette() }

@Composable
fun NiagaraCloneTheme(
    appTheme: AppTheme = AppTheme.PITCH_BLACK,
    accentColor: Color = Color(0xFF4DD0E1),
    content: @Composable () -> Unit
) {
    val palette = when (appTheme) {
        AppTheme.PITCH_BLACK -> LauncherPalette(
            theme = AppTheme.PITCH_BLACK,
            accent = accentColor,
            screenBackground = Color.Black,
            sheetBackground = Color(0xFF0D0D11),
            cardBackground = Color(0xFF16161C),
            borderColor = Color(0x26FFFFFF),
            textPrimary = Color.White,
            textSecondary = Color(0xFF9CA3AF),
            homeTextColor = Color.White,
            isLight = false,
            isGlass = false
        )
        AppTheme.LIGHT -> LauncherPalette(
            theme = AppTheme.LIGHT,
            accent = accentColor,
            screenBackground = Color(0xFFF4F4F8),
            sheetBackground = Color(0xFFFFFFFF),
            cardBackground = Color(0xFFEFEFF5),
            borderColor = Color(0x1F000000),
            textPrimary = Color(0xFF121216),
            textSecondary = Color(0xFF52525B),
            homeTextColor = Color.White,
            isLight = true,
            isGlass = false
        )
        AppTheme.GLASS -> {
            // True Frosted Glassmorphism: Dark refractive tint + luminous crystal border stroke
            val glassSheetBase = Color(0xFF161622).copy(alpha = 0.68f)
                .compositeOver(Color(0xFF080812).copy(alpha = 0.5f))
            val glassCardBase = Color(0xFF232336).copy(alpha = 0.58f)
                .compositeOver(accentColor.copy(alpha = 0.08f))
            
            LauncherPalette(
                theme = AppTheme.GLASS,
                accent = accentColor,
                screenBackground = Color.Transparent, // Wallpaper shines through fully
                sheetBackground = glassSheetBase,
                cardBackground = glassCardBase,
                borderColor = Color.White.copy(alpha = 0.28f), // Luminous glass reflection edge
                textPrimary = Color.White,
                textSecondary = Color(0xFFE4E4E7),
                homeTextColor = Color.White,
                isLight = false,
                isGlass = true
            )
        }
    }

    val colorScheme = if (palette.isLight) {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            background = palette.screenBackground,
            surface = palette.sheetBackground,
            surfaceVariant = palette.cardBackground,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary
        )
    } else {
        darkColorScheme(
            primary = accentColor,
            onPrimary = Color.Black,
            background = palette.screenBackground,
            surface = palette.sheetBackground,
            surfaceVariant = palette.cardBackground,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary
        )
    }

    CompositionLocalProvider(LocalLauncherPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            content = content
        )
    }
}

val AccentPresets = listOf(
    Color(0xFF4DD0E1), // Teal
    Color(0xFFD98CE0), // Niagara Orchid / Lavender
    Color(0xFF3B82F6), // Blue
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFFF43F5E), // Rose
    Color(0xFF8B5CF6)  // Violet
)
