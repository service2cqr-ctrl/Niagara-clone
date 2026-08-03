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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.exp

val INDEX_ITEMS = listOf("☆") + ('A'..'Z').map { it.toString() } + listOf("o")

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
            // VERTICAL & HORIZONTAL INSETS (Bezel Padding)
            .padding(
                top = 56.dp,
                bottom = 56.dp,
                start = if (isLeftHanded) 10.dp else 0.dp,
                end = if (!isLeftHanded) 10.dp else 0.dp
            )
            .width(32.dp)
            .layout { measurable, constraints ->
                totalHeight = constraints.maxHeight.toFloat()
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        touchY = offset.y
                        val indexFraction = (offset.y / totalHeight).coerceIn(0f, 1f)
                        val targetIndex = (indexFraction * (INDEX_ITEMS.size - 1)).toInt()
                        if (targetIndex in INDEX_ITEMS.indices && targetIndex != lastSelectedIndex) {
                            lastSelectedIndex = targetIndex
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCharSelected(INDEX_ITEMS[targetIndex])
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val currentY = (touchY ?: 0f) + dragAmount
                        touchY = currentY
                        val indexFraction = (currentY / totalHeight).coerceIn(0f, 1f)
                        val targetIndex = (indexFraction * (INDEX_ITEMS.size - 1)).toInt()
                        if (targetIndex in INDEX_ITEMS.indices && targetIndex != lastSelectedIndex) {
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
                        onDragEnd()
                    }
                )
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

                val distance = if (currentTouchY != null) abs(currentTouchY - itemY) else Float.MAX_VALUE
                val gaussianFactor = exp(-((distance * distance) / (2 * 4200f)))
                
                val rawOffset = gaussianFactor * 32f
                val horizontalOffset = if (isLeftHanded) rawOffset else -rawOffset
                
                val animatedOffset by animateFloatAsState(
                    targetValue = horizontalOffset,
                    animationSpec = spring(stiffness = 500f, dampingRatio = 0.6f),
                    label = "wave_offset"
                )

                val isSelected = distance < 28f

                Text(
                    text = char,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            blurRadius = 4f
                        )
                    ),
                    color = if (isSelected) Color(0xFF4DD0E1) else Color.White.copy(alpha = 0.85f),
                    fontSize = if (isSelected) 16.sp else 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.graphicsLayer {
                        translationX = animatedOffset
                    }
                )
            }
        }
    }
}
