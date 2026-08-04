package com.niagaraclone.launcher.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaPlayerCard(
    albumArt: ImageBitmap?,
    trackTitle: String,
    artistName: String,
    isPlaying: Boolean,
    isDarkMode: Boolean = true,
    onPlayPauseToggle: () -> Unit,
    onOpenApp: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Light / Dark Frosted Glass styling
    val backgroundColor = if (isDarkMode) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.55f)
    val borderColor = if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)
    val textColorPrimary = if (isDarkMode) Color.White else Color(0xFF1E1E1E)
    val textColorSecondary = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f)
    val equalizerBarColor = if (isDarkMode) Color(0xFF4DD0E1) else Color(0xFF00ACC1)

    // Gesture distance tracking for horizontal swipes
    var dragAmountTotal by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(32.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAmountTotal = 0f },
                    onDragEnd = {
                        if (dragAmountTotal > 60f) {
                            onNextTrack()
                        } else if (dragAmountTotal < -60f) {
                            onPreviousTrack()
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAmountTotal += dragAmount
                    }
                )
            }
            .clickable { onOpenApp() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- 1. ALBUM ART THUMBNAIL (TAP TO PLAY/PAUSE) ---
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDarkMode) Color(0xFF222222) else Color(0xFFE0E0E0))
                    .clickable { onPlayPauseToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (albumArt != null) {
                    Image(
                        bitmap = albumArt,
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music",
                        tint = textColorSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // --- 2. TRACK TITLE & ARTIST (WITH MARQUEE SCROLL) ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = trackTitle,
                    color = textColorPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = artistName,
                    color = textColorSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // --- 3. ANIMATED AUDIO EQUALIZER (SOUND WAVES) ---
            AnimatedEqualizer(
                isPlaying = isPlaying,
                barColor = equalizerBarColor,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}

@Composable
private fun AnimatedEqualizer(
    isPlaying: Boolean,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    // Generate independent height animations for 3 bars
    val h1 by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f, targetValue = 1.0f,
            animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
            label = "b1"
        )
    } else remember { mutableFloatStateOf(0.2f) }

    val h2 by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.8f, targetValue = 0.2f,
            animationSpec = infiniteRepeatable(tween(320, easing = LinearEasing), RepeatMode.Reverse),
            label = "b2"
        )
    } else remember { mutableFloatStateOf(0.4f) }

    val h3 by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f, targetValue = 0.9f,
            animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse),
            label = "b3"
        )
    } else remember { mutableFloatStateOf(0.2f) }

    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h1)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h2)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h3)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
    }
}
