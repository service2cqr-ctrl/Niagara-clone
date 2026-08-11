package com.niagaraclone.launcher.ui.components

import android.graphics.Rect
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt

val INDEX_ITEMS = listOf("⭐") + ('A'..'Z').map { it.toString() } + listOf("#")

@Composable
fun WaveSideIndex(
    modifier: Modifier = Modifier,
    activeChar: String? = null,
    notificationLetters: Set<String> = emptySet(),
    isIndexerVisible: Boolean = false,
    onTouchStart: () -> Unit = {},
    onCharSelected: (String) -> Unit = {},
    onDragEnd: (String) -> Unit = {},
    accentColor: Color = Color(0xFF4DD0E1)
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val hostView = LocalView.current

    var containerHeightPx by remember { mutableFloatStateOf(1f) }

    var isDragging by remember { mutableStateOf(false) }
    var touchY by remember { mutableStateOf<Float?>(null) }
    var touchX by remember { mutableFloatStateOf(0f) }
    var dragStartX by remember { mutableFloatStateOf(0f) }
    var activeEdgeIsLeft by remember { mutableStateOf(false) }
    var lastSelectedIndex by remember { mutableIntStateOf(-1) }

    val itemCenterYMap = remember { mutableStateMapOf<Int, Float>() }

    fun updateSelection(yPx: Float) {
        var closestIdx = 0
        var minDistance = Float.MAX_VALUE

        INDEX_ITEMS.indices.forEach { idx ->
            val centerY = itemCenterYMap[idx] ?: run {
                val fallbackFraction = (idx + 0.5f) / INDEX_ITEMS.size
                fallbackFraction * containerHeightPx
            }
            val dist = abs(yPx - centerY)
            if (dist < minDistance) {
                minDistance = dist
                closestIdx = idx
            }
        }

        if (closestIdx != lastSelectedIndex) {
            lastSelectedIndex = closestIdx
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onCharSelected(INDEX_ITEMS[closestIdx])
        }
    }

    val createEdgePointerInput: suspend PointerInputScope.(Boolean) -> Unit = { isLeft ->
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            isDragging = true
            activeEdgeIsLeft = isLeft
            dragStartX = down.position.x
            touchX = down.position.x
            touchY = down.position.y.coerceIn(0f, containerHeightPx)

            onTouchStart()
            updateSelection(down.position.y)

            do {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (change.pressed) {
                    change.consume()
                    touchX = change.position.x
                    touchY = change.position.y.coerceIn(0f, containerHeightPx)
                    updateSelection(change.position.y)
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
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerHeightPx = coordinates.size.height.toFloat()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val bounds = coordinates.boundsInWindow()
                    val stripWidthPx = with(density) { 44.dp.toPx() }.toInt()

                    val leftRect = Rect(0, bounds.top.toInt(), stripWidthPx, bounds.bottom.toInt())
                    val rightRect = Rect((bounds.right - stripWidthPx).toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt())

                    hostView.systemGestureExclusionRects = listOf(leftRect, rightRect)
                }
            }
    ) {
        // --- 1. LEFT EDGE TOUCH STRIP ---
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(44.dp)
                .pointerInput(Unit) { createEdgePointerInput(true) }
        )

        // --- 2. RIGHT EDGE TOUCH STRIP ---
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(44.dp)
                .pointerInput(Unit) { createEdgePointerInput(false) }
        )

        // --- 3. INDEXER ARC RENDER (Niagara Ergonomic Padding) ---
        if (isIndexerVisible || isDragging) {
            val currentY = touchY

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = 165.dp, bottom = 85.dp, end = 12.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.End
            ) {
                INDEX_ITEMS.forEachIndexed { index, char ->
                    val itemCenterY = itemCenterYMap[index] ?: 0f
                    val distance = if (currentY != null) abs(currentY - itemCenterY) else Float.MAX_VALUE

                    val influenceRadiusPx = with(density) { 180.dp.toPx() }
                    val circularArcFactor = if (distance < influenceRadiusPx) {
                        val cosVal = cos((PI.toFloat() * distance) / (2f * influenceRadiusPx))
                        cosVal * cosVal
                    } else {
                        0f
                    }

                    val pullDistancePx = if (activeEdgeIsLeft) {
                        (touchX - dragStartX).coerceAtLeast(0f)
                    } else {
                        (dragStartX - touchX).coerceAtLeast(0f)
                    }

                    val maxLimitPx = with(density) { 200.dp.toPx() }
                    val actualMaxDisplacement = pullDistancePx.coerceIn(with(density) { 36.dp.toPx() }, maxLimitPx)

                    val targetTranslationX = -1f * (actualMaxDisplacement * circularArcFactor)
                    val targetScale = 1.0f + (0.75f * circularArcFactor)

                    val animatedOffset by animateFloatAsState(
                        targetValue = targetTranslationX,
                        animationSpec = spring(stiffness = 450f, dampingRatio = 0.7f),
                        label = "wave_offset"
                    )

                    val animatedScale by animateFloatAsState(
                        targetValue = targetScale,
                        animationSpec = spring(stiffness = 450f, dampingRatio = 0.7f),
                        label = "wave_scale"
                    )

                    val isSelected = index == lastSelectedIndex || char == activeChar
                    val hasNotification = notificationLetters.contains(char)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                val bounds = coords.boundsInParent()
                                itemCenterYMap[index] = bounds.top + bounds.height / 2f
                            }
                            .graphicsLayer {
                                translationX = animatedOffset
                                scaleX = animatedScale
                                scaleY = animatedScale
                            }
                    ) {
                        if (hasNotification) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(5.dp)
                                    .background(accentColor, CircleShape)
                            )
                        }

                        Text(
                            text = char,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    blurRadius = 6f
                                )
                            ),
                            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- 4. DRAG PREVIEW BADGE ---
        if (isDragging && touchY != null) {
            val currentActiveChar = if (lastSelectedIndex in INDEX_ITEMS.indices) INDEX_ITEMS[lastSelectedIndex] else "⭐"
            val badgeOffsetY = ((touchY ?: 0f) - with(density) { 28.dp.toPx() }).roundToInt()

            val pullDistancePx = if (activeEdgeIsLeft) {
                (touchX - dragStartX).coerceAtLeast(0f)
            } else {
                (dragStartX - touchX).coerceAtLeast(0f)
            }
            val maxLimitPx = with(density) { 200.dp.toPx() }
            val apexXDisplacement = pullDistancePx.coerceIn(with(density) { 36.dp.toPx() }, maxLimitPx)

            val badgeOffsetX = - (apexXDisplacement + with(density) { 48.dp.toPx() })

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(x = badgeOffsetX.roundToInt(), y = badgeOffsetY) }
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor,
                    shadowElevation = 10.dp,
                    modifier = Modifier.size(54.dp)
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
