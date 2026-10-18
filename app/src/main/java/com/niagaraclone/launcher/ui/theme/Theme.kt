package com.niagaraclone.launcher.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AppTheme {
    PITCH_BLACK,
    LIGHT,
    GLASS
}

@Immutable
data class LauncherPalette(
    val theme: AppTheme,
    val screenBackground: Color,
    val sheetBackground: Color,
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val borderColor: Color,
    val accent: Color
) {
    val isPitchBlack: Boolean get() = theme == AppTheme.PITCH_BLACK
    val isLight: Boolean get() = theme == AppTheme.LIGHT
    val isGlass: Boolean get() = theme == AppTheme.GLASS
}

val LocalLauncherPalette = staticCompositionLocalOf {
    LauncherPalette(
        theme = AppTheme.PITCH_BLACK,
        screenBackground = Color.Black,
        sheetBackground = Color(0xFF121214),
        cardBackground = Color(0xFF1C1C1F),
        textPrimary = Color.White,
        textSecondary = Color(0xFF9E9EA8),
        borderColor = Color.White.copy(alpha = 0.08f),
        accent = Color(0xFFD98CE0)
    )
}

/**
 * True Optical Glassmorphic Surface Modifier:
 * In GLASS mode, renders a multi-stop diagonal specular frost gradient +
 * a directional crystal border (bright top-left light catch, subtle bottom-right edge).
 */
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(20.dp),
    isSheet: Boolean = false,
    borderWidth: Dp = 1.dp
): Modifier = composed {
    val palette = LocalLauncherPalette.current
    if (palette.isGlass) {
        val topLeftTint = Color.White.copy(alpha = if (isSheet) 0.22f else 0.20f)
            .compositeOver(palette.accent.copy(alpha = 0.14f))
        val centerTint = Color(0xFF141824).copy(alpha = if (isSheet) 0.62f else 0.44f)
        val bottomRightTint = Color(0xFF0A0D14).copy(alpha = if (isSheet) 0.74f else 0.56f)

        val borderBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.52f),
                palette.accent.copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.10f)
            ),
            start = Offset(0f, 0f),
            end = Offset(1000f, 1000f)
        )

        this
            .clip(shape)
            .drawWithCache {
                val bgBrush = Brush.linearGradient(
                    colors = listOf(topLeftTint, centerTint, bottomRightTint),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
                onDrawBehind {
                    drawRect(brush = bgBrush)
                }
            }
            .border(width = borderWidth, brush = borderBrush, shape = shape)
    } else {
        val bgColor = if (isSheet) palette.sheetBackground else palette.cardBackground
        this
            .clip(shape)
            .background(bgColor, shape)
            .border(width = borderWidth, color = palette.borderColor, shape = shape)
    }
}

@Composable
fun NiagaraCloneTheme(
    appTheme: AppTheme = AppTheme.PITCH_BLACK,
    accentColor: Color = Color(0xFFD98CE0),
    content: @Composable () -> Unit
) {
    val palette = when (appTheme) {
        AppTheme.PITCH_BLACK -> LauncherPalette(
            theme = AppTheme.PITCH_BLACK,
            screenBackground = Color.Black,
            sheetBackground = Color(0xFF0E0E10),
            cardBackground = Color(0xFF18181B),
            textPrimary = Color.White,
            textSecondary = Color(0xFFA1A1AA),
            borderColor = Color.White.copy(alpha = 0.08f),
            accent = accentColor
        )

        AppTheme.LIGHT -> LauncherPalette(
            theme = AppTheme.LIGHT,
            screenBackground = Color(0xFFF4F5F8),
            sheetBackground = Color(0xFFFFFFFF),
            cardBackground = Color(0xFFEAECEF),
            textPrimary = Color(0xFF121316),
            textSecondary = Color(0xFF5A5D65),
            borderColor = Color.Black.copy(alpha = 0.09f),
            accent = accentColor
        )

        AppTheme.GLASS -> LauncherPalette(
            theme = AppTheme.GLASS,
            // Translucent screen background so the live system wallpaper shines through Settings/Widgets/Drawer!
            screenBackground = Color(0xFF06080E).copy(alpha = 0.42f),
            // Translucent frosted base for ModalBottomSheets & AlertDialogs
            sheetBackground = accentColor.copy(alpha = 0.14f)
                .compositeOver(Color(0xFF10141F).copy(alpha = 0.64f)),
            // Translucent crystal base for Cards, Search Bars & Rows
            cardBackground = Color.White.copy(alpha = 0.12f)
                .compositeOver(Color(0xFF161B28).copy(alpha = 0.46f)),
            textPrimary = Color.White,
            textSecondary = Color.White.copy(alpha = 0.78f),
            // Crisp specular edge highlight
            borderColor = Color.White.copy(alpha = 0.36f),
            accent = accentColor
        )
    }

    val colorScheme = if (appTheme == AppTheme.LIGHT) {
        lightColorScheme(
            primary = accentColor,
            secondary = accentColor,
            background = palette.screenBackground,
            surface = palette.sheetBackground,
            onPrimary = Color.Black,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary
        )
    } else {
        darkColorScheme(
            primary = accentColor,
            secondary = accentColor,
            background = palette.screenBackground,
            surface = palette.sheetBackground,
            onPrimary = Color.Black,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary
        )
    }

    CompositionLocalProvider(LocalLauncherPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
