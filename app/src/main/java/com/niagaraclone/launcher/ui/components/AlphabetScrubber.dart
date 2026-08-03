package com.cascadia.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

@Composable
fun AlphabetScrubber(
    alphabet: List<String>,
    onLetterSelected: (String) -> Unit,
    onSelectStar: () -> Unit,
    onSelectDot: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var selectedLetter by remember { mutableStateOf("") }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .width(70.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        touchY = offset.y
                    },
                    onVerticalDrag = { change, _ ->
                        touchY = change.position.y
                    },
                    onDragEnd = {
                        isDragging = false
                        onDragEnd()
                    },
                    onDragCancel = {
                        isDragging = false
                        onDragEnd()
                    }
                )
            }
    ) {
        val heightPx = with(density) { maxHeight.toPx() }
        val itemHeightPx = heightPx / alphabet.size

        // Calculate active letter based on touch position
        LaunchedEffect(touchY, isDragging) {
            if (isDragging && alphabet.isNotEmpty()) {
                val index = (touchY / itemHeightPx).toInt().coerceIn(0, alphabet.size - 1)
                val letter = alphabet[index]
                if (selectedLetter != letter) {
                    selectedLetter = letter
                    when (letter) {
                        "★" -> onSelectStar()
                        "∘" -> onSelectDot()
                        else -> onLetterSelected(letter)
                    }
                }
            }
        }

        // 1. Draw the Alphabet List with the Arc Curve
        Canvas(modifier = Modifier.fillMaxSize()) {
            val defaultX = size.width - 20dp.toPx()

            alphabet.forEachIndexed { i, item ->
                val itemY = (i * itemHeightPx) + (itemHeightPx / 2)
                var offsetX = 0f

                // Gaussian Curve calculation around touch position
                if (isDragging) {
                    val dist = abs(itemY - touchY)
                    val maxDist = 120dp.toPx()
                    if (dist < maxDist) {
                        offsetX = (45dp.toPx() * cos((dist / maxDist) * (PI / 2))).toFloat()
                    }
                }

                val measuredText = textMeasurer.measure(
                    text = item,
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                val renderX = defaultX - offsetX - (measuredText.size.width / 2)
                val renderY = itemY - (measuredText.size.height / 2)

                drawText(
                    textLayoutResult = measuredText,
                    topLeft = Offset(renderX, renderY)
                )
            }
        }

        // 2. Active Selected Letter Floating Bubble
        if (isDragging && selectedLetter.isNotEmpty()) {
            val badgeOffsetYDp = with(density) { touchY.toDp() } - 24.dp

            Box(
                modifier = Modifier
                    .offset(x = (-20).dp, y = badgeOffsetYDp)
                    .size(48.dp)
                    .background(Color(0xFF80CBC4), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedLetter,
                    style = TextStyle(
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
