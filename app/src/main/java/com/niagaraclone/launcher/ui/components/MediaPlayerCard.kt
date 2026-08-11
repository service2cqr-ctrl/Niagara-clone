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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.MediaWidgetTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaPlayerCard(
    albumArt: ImageBitmap?,
    trackTitle: String,
    artistName: String,
    isPlaying: Boolean,
    widgetTheme: MediaWidgetTheme = MediaWidgetTheme.FROSTED_GLASS,
    onPlayPauseToggle: () -> Unit,
    onOpenApp: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (widgetTheme) {
        MediaWidgetTheme.LIGHT -> Color.White.copy(alpha = 0.85f)
        MediaWidgetTheme.DARK -> Color(0xFF1E1E1E).copy(alpha = 0.90f)
        MediaWidgetTheme.FROSTED_GLASS, MediaWidgetTheme.MATCH_ACCENT, MediaWidgetTheme.ALBUM_ART -> Color.White.copy(alpha = 0.22f)
    }

    val borderBrush = when (widgetTheme) {
        MediaWidgetTheme.LIGHT -> Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.05f))
        )
        MediaWidgetTheme.DARK -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.05f))
        )
        else -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.15f))
        )
    }

    val textColorPrimary = when (widgetTheme) {
        MediaWidgetTheme.LIGHT -> Color(0xFF111111)
        else -> Color.White
    }

    val textColorSecondary = when (widgetTheme) {
        MediaWidgetTheme.LIGHT -> Color.Black.copy(alpha = 0.6f)
        else -> Color.White.copy(alpha = 0.75f)
    }

    val equalizerColor = when (widgetTheme) {
        MediaWidgetTheme.LIGHT -> Color.Black.copy(alpha = 0.8f)
        else -> Color.White
    }

    var dragAmountTotal by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.2.dp, borderBrush, CircleShape)
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
            .padding(start = 6.dp, end = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
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
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = trackTitle,
                    color = textColorPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = artistName,
                    color = textColorSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AnimatedEqualizer(
                isPlaying = isPlaying,
                barColor = equalizerColor
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

    val h1 by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.25f, targetValue = 0.95f,
            animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "bar1"
        )
    } else remember { mutableFloatStateOf(0.2f) }

    val h2 by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.85f, targetValue = 0.15f,
            animationSpec = infiniteRepeatable(tween(300, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
            label = "bar2"
        )
    } else remember { mutableFloatStateOf(0.4f) }

    val h3 by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.35f, targetValue = 0.90f,
            animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "bar3"
        )
    } else remember { mutableFloatStateOf(0.25f) }

    val h4 by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.70f, targetValue = 0.20f,
            animationSpec = infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse),
            label = "bar4"
        )
    } else remember { mutableFloatStateOf(0.35f) }

    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(h1, h2, h3, h4).forEach { heightFraction ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .fillMaxHeight(heightFraction)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}
