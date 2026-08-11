package com.niagaraclone.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

val INDEX_ITEMS = listOf("⭐") + ('A'..'Z').map { it.toString() } + listOf("o")

@Composable
fun WaveSideIndex(
    modifier: Modifier = Modifier,
    activeChar: String? = null,
    isIndexerVisible: Boolean = false,
    onTouchStart: () -> Unit = {},
    onCharSelected: (String) -> Unit = {},
    onDragEnd: (String) -> Unit = {},
    accentColor: Color = Color(0xFF4DD0E1)
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    var componentHeightPx by remember { mutableFloatStateOf(1f) }
    var componentWidthPx by remember { mutableFloatStateOf(1f) }

    var isDragging by remember { mutableStateOf(false) }
    var touchY by remember { mutableStateOf<Float?>(null) }
    var touchX by remember { mutableFloatStateOf(0f) }
    var dragStartX by remember { mutableFloatStateOf(0f) }
    var lastSelectedIndex by remember { mutableIntStateOf(-1) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp) // Wide gesture touch target
            .onGloballyPositioned { coordinates ->
                componentHeightPx = coordinates.size.height.toFloat()
                componentWidthPx = coordinates.size.width.toFloat()
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isDragging = true
                    dragStartX = down.position.x
                    touchX = down.position.x
                    touchY = down.position.y.coerceIn(0f, componentHeightPx)

                    onTouchStart()

                    val topPaddingPx = with(density) { 140.dp.toPx() }
                    val bottomPaddingPx = with(density) { 40.dp.toPx() }
                    val usableHeightPx = (componentHeightPx - topPaddingPx - bottomPaddingPx).coerceAtLeast(1f)
                    val adjustedY = (down.position.y - topPaddingPx).coerceIn(0f, usableHeightPx)

                    val initialFraction = adjustedY / usableHeightPx
                    val initialIndex = (initialFraction * (INDEX_ITEMS.size - 1)).roundToInt().coerceIn(INDEX_ITEMS.indices)

                    if (initialIndex != lastSelectedIndex) {
                        lastSelectedIndex = initialIndex
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCharSelected(INDEX_ITEMS[initialIndex])
                    }

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            change.consume()
                            touchX = change.position.x
                            touchY = change.position.y.coerceIn(0f, componentHeightPx)

                            val currentAdjustedY = (change.position.y - topPaddingPx).coerceIn(0f, usableHeightPx)
                            val fraction = currentAdjustedY / usableHeightPx
                            val targetIndex = (fraction * (INDEX_ITEMS.size - 1)).roundToInt().coerceIn(INDEX_ITEMS.indices)

                            if (targetIndex != lastSelectedIndex) {
                                lastSelectedIndex = targetIndex
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCharSelected(INDEX_ITEMS[targetIndex])
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    val finalChar = if (lastSelectedIndex in INDEX_ITEMS.indices) {
                        INDEX_ITEMS[lastSelectedIndex]
                    } else {
                        "⭐"
                    }
                    isDragging = false
                    touchY = null
                    lastSelectedIndex = -1
                    onDragEnd(finalChar)
                }
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        if (isIndexerVisible || isDragging) {
            val currentY = touchY

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 140.dp, bottom = 40.dp, end = 20.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.End
            ) {
                INDEX_ITEMS.forEachIndexed { index, char ->
                    val topPaddingPx = with(density) { 140.dp.toPx() }
                    val usableHeightPx = componentHeightPx - with(density) { 180.dp.toPx() }
                    val itemY = topPaddingPx + (index.toFloat() / (INDEX_ITEMS.size - 1)) * usableHeightPx

                    val distance = if (currentY != null) abs(currentY - itemY) else Float.MAX_VALUE
                    val gaussianFactor = exp(-((distance * distance) / (2 * 5000f)))

                    // Asymmetric Displacement Physics
                    val isRightToLeftDrag = dragStartX > (componentWidthPx / 2f)
                    val maxDisplacementPx = if (isRightToLeftDrag) {
                        (componentWidthPx - touchX).coerceIn(0f, with(density) { 180.dp.toPx() })
                    } else {
                        (touchX - dragStartX).coerceIn(0f, with(density) { 100.dp.toPx() })
                    }

                    val targetTranslationX = -1f * (maxDisplacementPx * gaussianFactor)
                    val targetScale = 1.0f + (0.75f * gaussianFactor)

                    val animatedOffset by animateFloatAsState(
                        targetValue = targetTranslationX,
                        animationSpec = spring(stiffness = 500f, dampingRatio = 0.65f),
                        label = "wave_offset"
                    )

                    val animatedScale by animateFloatAsState(
                        targetValue = targetScale,
                        animationSpec = spring(stiffness = 500f, dampingRatio = 0.65f),
                        label = "wave_scale"
                    )

                    val isSelected = (currentY != null && distance < 28f) || char == activeChar

                    Text(
                        text = char,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                blurRadius = 4f
                            )
                        ),
                        color = if (isSelected) accentColor else Color.White.copy(alpha = 0.85f),
                        fontSize = if (isSelected) 16.sp else 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.graphicsLayer {
                            translationX = animatedOffset
                            scaleX = animatedScale
                            scaleY = animatedScale
                        }
                    )
                }
            }
        }

        // Floating Active Letter Badge Tracking Touch Height
        if (isDragging && touchY != null) {
            val currentActiveChar = if (lastSelectedIndex in INDEX_ITEMS.indices) INDEX_ITEMS[lastSelectedIndex] else "⭐"
            val badgeOffsetY = ((touchY ?: 0f) - with(density) { 28.dp.toPx() }).roundToInt()

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(x = -with(density) { 72.dp.toPx() }.roundToInt(), y = badgeOffsetY) }
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = currentActiveChar,
                            color = Color.Black,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
