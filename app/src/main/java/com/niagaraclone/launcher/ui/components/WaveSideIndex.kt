package com.niagaraclone.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.exp

val INDEX_ITEMS = listOf("⭐") + ('A'..'Z').map { it.toString() } + listOf("o")

/**
 * Niagara-style wave side index component for quick alphabet jump scrolling.
 * Supports both right-handed and left-handed edge curve projections.
 */
@Composable
fun WaveSideIndex(
    modifier: Modifier = Modifier,
    isLeftHanded: Boolean = false,
    onCharSelected: (String) -> Unit,
    onDragEnd: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var touchY by remember { mutableStateOf<Float?>(null) }
    var lastSelectedIndex by remember { mutableIntStateOf(-1) }
    var totalHeight by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(120.dp) // Wide touch hit area for the fluid wave arc
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        touchY = offset.y
                        val indexFraction = (offset.y / totalHeight).coerceIn(0f, 1f)
                        val targetIndex = (indexFraction * (INDEX_ITEMS.size - 1)).toInt()

                        if (targetIndex in INDEX_ITEMS.indices) {
                            lastSelectedIndex = targetIndex
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCharSelected(INDEX_ITEMS[targetIndex])
                        }
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
        contentAlignment = if (isLeftHanded) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = if (isLeftHanded) Alignment.Start else Alignment.End
        ) {
            INDEX_ITEMS.forEachIndexed { index, char ->
                val itemY = (index.toFloat() / INDEX_ITEMS.size) * totalHeight
                val currentTouchY = touchY

                // Gaussian curve calculation for wave effect (X-offset and Scale)
                val distance = if (currentTouchY != null) abs(currentTouchY - itemY) else Float.MAX_VALUE
                val waveOffset = if (currentTouchY != null && distance < 250f) {
                    (exp(-((distance * distance) / (2 * 60f * 60f))) * 70f)
                } else {
                    0f
                }

                val scale = if (currentTouchY != null && distance < 180f) {
                    1f + (exp(-((distance * distance) / (2 * 50f * 50f))) * 1.2f)
                } else {
                    1f
                }

                // Invert offset direction if left-handed
                val targetXOffset = if (isLeftHanded) waveOffset else -waveOffset

                val animatedXOffset by animateFloatAsState(
                    targetValue = targetXOffset,
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
                        .padding(start = if (isLeftHanded) 12.dp else 0.dp, end = if (isLeftHanded) 0.dp else 12.dp)
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
