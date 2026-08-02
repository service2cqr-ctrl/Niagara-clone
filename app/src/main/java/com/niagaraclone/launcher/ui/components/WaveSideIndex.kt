package com.niagaraclone.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.exp

val INDEX_ITEMS = listOf("⭐") + ('A'..'Z').map { it.toString() } + listOf("o")

@Composable
fun WaveSideIndex(
    modifier: Modifier = Modifier,
    onCharSelected: (String) -> Unit,
    onDragEnd: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var touchY by remember { mutableStateOf<Float?>(null) }
    var lastSelectedIndex by remember { mutableStateOf(-1) }
    var totalHeight by remember { mutableStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(120.dp) // Wide area to accommodate the leftward arc
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        touchY = offset.y
                    },
                    onDragEnd = {
                        touchY = null
                        lastSelectedIndex = -1
                        onDragEnd()
                    },
                    onDragCancel = {
                        touchY = null
                        lastSelectedIndex = -1
                    },
                    onVerticalDrag = { change, _ ->
                        touchY = change.position.y
                        val indexFraction = (change.position.y / totalHeight).coerceIn(0f, 1f)
                        val targetIndex = (indexFraction * (INDEX_ITEMS.size - 1)).toInt()

                        if (targetIndex != lastSelectedIndex && targetIndex in INDEX_ITEMS.indices) {
                            lastSelectedIndex = targetIndex
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCharSelected(INDEX_ITEMS[targetIndex])
                        }
                    }
                )
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                totalHeight = constraints.maxHeight.toFloat()
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.End
        ) {
            INDEX_ITEMS.forEachIndexed { index, char ->
                val itemY = (index.toFloat() / INDEX_ITEMS.size) * totalHeight
                val currentTouchY = touchY

                // Gaussian curve calculation for wave effect (X-offset and Scale)
                val distance = if (currentTouchY != null) abs(currentTouchY - itemY) else Float.MAX_VALUE
                val waveOffset = if (currentTouchY != null && distance < 250f) {
                    (exp(-((distance * distance) / (2 * 60f * 60f))) * 70f) // Max 70dp arc to left
                } else {
                    0f
                }

                val scale = if (currentTouchY != null && distance < 180f) {
                    1f + (exp(-((distance * distance) / (2 * 50f * 50f))) * 1.2f) // Scale up active item
                } else {
                    1f
                }

                val animatedXOffset by animateFloatAsState(
                    targetValue = -waveOffset,
                    animationSpec = spring(stiffness = 800f),
                    label = "waveOffset"
                )
                val animatedScale by animateFloatAsState(
                    targetValue = scale,
                    animationSpec = spring(stiffness = 800f),
                    label = "waveScale"
                )

                Text(
                    text = char,
                    color = if (index == lastSelectedIndex) Color(0xFF4DD0E1) else Color.White.copy(alpha = 0.8f),
                    fontSize = if (char == "⭐" || char == "o") 11.sp else 10.sp,
                    fontWeight = if (index == lastSelectedIndex) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .graphicsLayer {
                            translationX = animatedXOffset.dp.toPx()
                            scaleX = animatedScale
                            scaleY = animatedScale
                        }
                )
            }
        }
    }
}
