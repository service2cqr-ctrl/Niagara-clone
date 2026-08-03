package com.niagaraclone.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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

val INDEX_ITEMS = listOf("⭐") + ('A'..'Z').map { it.toString() } + listOf("o")

@Composable
fun WaveSideIndex(
    modifier: Modifier = Modifier,
    activeChar: String? = null,
    isIndexerVisible: Boolean = false,
    onTouchStart: () -> Unit = {}, // Added default value here,,
    onCharSelected: (String) -> Unit = {},
    onDragEnd: (String) -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    var touchY by remember { mutableStateOf<Float?>(null) }
    var lastSelectedIndex by remember { mutableIntStateOf(-1) }
    var totalHeight by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(
                top = 170.dp,
                bottom = 50.dp,
                end = 24.dp
            )
            .width(48.dp) // Edge touch zone
            .layout { measurable, constraints ->
                totalHeight = constraints.maxHeight.toFloat()
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    // 1. TOUCH DOWN (Triggers drawer & indexer immediately)
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touchY = down.position.y
                    onTouchStart()
                    
                    val initialFraction = (down.position.y / totalHeight).coerceIn(0f, 1f)
                    val initialIndex = (initialFraction * (INDEX_ITEMS.size - 1)).toInt()
                    
                    if (initialIndex in INDEX_ITEMS.indices) {
                        lastSelectedIndex = initialIndex
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCharSelected(INDEX_ITEMS[initialIndex])
                    }

                    // 2. UNBROKEN CONTINUOUS DRAG
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            change.consume()
                            touchY = change.position.y
                            val fraction = (change.position.y / totalHeight).coerceIn(0f, 1f)
                            val targetIndex = (fraction * (INDEX_ITEMS.size - 1)).toInt()
                            
                            if (targetIndex in INDEX_ITEMS.indices && targetIndex != lastSelectedIndex) {
                                lastSelectedIndex = targetIndex
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCharSelected(INDEX_ITEMS[targetIndex])
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    // 3. FINGER LIFT
                    val finalChar = if (lastSelectedIndex in INDEX_ITEMS.indices) {
                        INDEX_ITEMS[lastSelectedIndex]
                    } else {
                        "⭐"
                    }
                    touchY = null
                    lastSelectedIndex = -1
                    onDragEnd(finalChar)
                }
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        if (isIndexerVisible || touchY != null) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.End
            ) {
                INDEX_ITEMS.forEachIndexed { index, char ->
                    val itemY = (index.toFloat() / INDEX_ITEMS.size) * totalHeight
                    val currentTouchY = touchY

                    val distance = if (currentTouchY != null) abs(currentTouchY - itemY) else Float.MAX_VALUE
                    val gaussianFactor = exp(-((distance * distance) / (2 * 4200f)))
                    val horizontalOffset = -gaussianFactor * 32f

                    val animatedOffset by animateFloatAsState(
                        targetValue = horizontalOffset,
                        animationSpec = spring(stiffness = 500f, dampingRatio = 0.6f),
                        label = "wave_offset"
                    )

                    val isSelected = distance < 28f || char == activeChar

                    Text(
                        text = char,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                blurRadius = 4f
                            )
                        ),
                        color = if (isSelected) Color(0xFF4DD0E1) else Color.White.copy(alpha = 0.85f),
                        fontSize = if (isSelected) 18.sp else 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.graphicsLayer {
                            translationX = animatedOffset
                        }
                    )
                }
            }
        }
    }
}
