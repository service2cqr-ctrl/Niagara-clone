package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.MediaWidgetTheme
import com.niagaraclone.launcher.ui.theme.LocalLauncherPalette
import com.niagaraclone.launcher.ui.theme.glassSurface

@Composable
fun MediaPlayerCard(
    albumArt: ImageBitmap?,
    trackTitle: String,
    artistName: String,
    isPlaying: Boolean,
    widgetTheme: MediaWidgetTheme,
    accentColor: Color,
    onPlayPauseToggle: () -> Unit,
    onOpenApp: () -> Unit,
    onNextTrack: () -> Unit = {},
    onPreviousTrack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalLauncherPalette.current
    val useGlassEffect = palette.isGlass || widgetTheme == MediaWidgetTheme.FROSTED_GLASS
    val cardShape = RoundedCornerShape(24.dp)

    val containerColor = when {
        useGlassEffect -> Color.Transparent
        widgetTheme == MediaWidgetTheme.LIGHT -> Color(0xFFF2F2F7)
        widgetTheme == MediaWidgetTheme.MATCH_ACCENT -> accentColor.copy(alpha = 0.22f)
        else -> palette.cardBackground
    }

    val primaryTextColor = when {
        useGlassEffect -> Color.White
        widgetTheme == MediaWidgetTheme.LIGHT -> Color.Black
        else -> palette.textPrimary
    }

    val secondaryTextColor = when {
        useGlassEffect -> Color.White.copy(alpha = 0.78f)
        widgetTheme == MediaWidgetTheme.LIGHT -> Color.DarkGray
        else -> palette.textSecondary
    }

    val cardModifier = if (useGlassEffect) {
        modifier
            .fillMaxWidth()
            .glassSurface(shape = cardShape, isSheet = false, borderWidth = 1.2.dp)
            .clickable { onOpenApp() }
    } else {
        modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(containerColor, cardShape)
            .border(1.dp, palette.borderColor, cardShape)
            .clickable { onOpenApp() }
    }

    Box(modifier = cardModifier) {
        // Optional subtle album art backdrop tint
        if (widgetTheme == MediaWidgetTheme.ALBUM_ART && albumArt != null) {
            Image(
                bitmap = albumArt,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.28f,
                modifier = Modifier.matchParentSize()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art Thumbnail with crystal rim
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = if (useGlassEffect) 0.35f else 0.1f)
                ),
                modifier = Modifier.size(52.dp)
            ) {
                if (albumArt != null) {
                    Image(
                        bitmap = albumArt,
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Track & Artist Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trackTitle,
                    color = primaryTextColor,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (artistName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = artistName,
                        color = secondaryTextColor,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Transport Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onPreviousTrack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = primaryTextColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Frosted Glass Play/Pause Pill
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    accentColor,
                                    accentColor.copy(alpha = 0.8f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(100f, 100f)
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                        .clickable { onPlayPauseToggle() }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onNextTrack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = primaryTextColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
