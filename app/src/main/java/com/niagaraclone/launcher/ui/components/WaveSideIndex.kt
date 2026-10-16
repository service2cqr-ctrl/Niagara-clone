package com.niagaraclone.launcher.ui.components

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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt

val INDEX_ITEMS = listOf("☆") + ('A'..'Z').map { it.toString() } + listOf("∘")

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

    // Ensure Android system back gesture is NEVER blocked by exclusion rects
    DisposableEffect(hostView) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            hostView.systemGestureExclusionRects = emptyList()
        }
        onDispose {}
    }

    var containerHeightPx by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }
    var touchY by remember { mutableStateOf<Float?>(null) }
    var pullFactor by remember { mutableFloatStateOf(1f) }
    var lastSelectedIndex by remember { mutableIntStateOf(-1) }

    val itemCenterYMap = remember { mutableStateMapOf<Int, Float>() }

    val topPaddingDp = 175.dp
    val bottomPaddingDp = 95.dp

    fun updateSelection(rootY: Float) {
        var closestIdx = 0
        var minDistance = Float.MAX_VALUE

        INDEX_ITEMS.indices.forEach { idx ->
            val centerY = itemCenterYMap[idx] ?: run {
                val topPx = with(density) { topPaddingDp.toPx() }
                val bottomPx = with(density) { bottomPaddingDp.toPx() }
                val availableHeight = (containerHeightPx - topPx - bottomPx).coerceAtLeast(1f)
                topPx + ((idx + 0.5f) / INDEX_ITEMS.size) * availableHeight
            }
            val dist = abs(rootY - centerY)
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

    val handleIndexPointerInput: suspend PointerInputScope.() -> Unit = {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val topBoundPx = with(density) { topPaddingDp.toPx() }
            val bottomBoundPx = containerHeightPx - with(density) { bottomPaddingDp.toPx() }

            // Ignore touches outside the vertical alphabet strip bounds
            if (down.position.y < topBoundPx || down.position.y > bottomBoundPx) {
                return@awaitEachGesture
            }

            val startX = down.position.x
            val startY = down.position.y
            var gestureActivated = false
            var isSystemBackSwipe = false

            // Wait up to 90ms to distinguish a fast horizontal Back swipe from a vertical scrub / hold
            val slopResult = withTimeoutOrNull(90L) {
                var result = false
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    if (!change.pressed) {
                        // Quick tap on the alphabet bar
                        result = true
                        break
                    }
                    val dx = abs(change.position.x - startX)
                    val dy = abs(change.position.y - startY)

                    // Horizontal swipe inward -> let Android handle system Back gesture!
                    if (dx > with(density) { 14.dp.toPx() } && dx > dy * 1.4f) {
                        isSystemBackSwipe = true
                        break
                    }
                    // Vertical scrub -> activate wave indexer immediately
                    if (dy > with(density) { 6.dp.toPx() }) {
                        change.consume()
                        result = true
                        break
                    }
                }
                result
            }

            if (isSystemBackSwipe) {
                return@awaitEachGesture
            }

            gestureActivated = slopResult ?: true

            if (gestureActivated) {
                isDragging = true
                touchY = startY.coerceIn(topBoundPx, bottomBoundPx)
                pullFactor = 1f
                onTouchStart()
                updateSelection(touchY!!)

                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    if (change.pressed) {
                        change.consume()
                        touchY = change.position.y.coerceIn(topBoundPx, bottomBoundPx)
                        val extraPullPx = (startX - change.position.x).coerceAtLeast(0f)
                        val maxExtraPullPx = with(density) { 80.dp.toPx() }
                        pullFactor = 1f + (extraPullPx / maxExtraPullPx).coerceIn(0f, 0.35f)
                        updateSelection(touchY!!)
                    }
                } while (event.changes.any { it.pressed })

                val finalChar = if (lastSelectedIndex in INDEX_ITEMS.indices) {
                    INDEX_ITEMS[lastSelectedIndex]
                } else {
                    "☆"
                }
                isDragging = false
                touchY = null
                lastSelectedIndex = -1
                onDragEnd(finalChar)
            }
        }
    }

    // Wide wave parameters matching Niagara Launcher's smooth bow arc
    val waveRadiusPx = with(density) { 260.dp.toPx() }
    val basePeakOffsetPx = with(density) { 92.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerHeightPx = coordinates.size.height.toFloat()
            }
    ) {
        // Right-edge touch strip (inset by 10.dp from the physical screen edge so system back-swipe from bezel passes through cleanly)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(end = 10.dp)
                .width(34.dp)
                .pointerInput(Unit, handleIndexPointerInput)
        )

        val currentY = touchY

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = topPaddingDp, bottom = bottomPaddingDp, end = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            INDEX_ITEMS.forEachIndexed { index, char ->
                val itemCenterY = itemCenterYMap[index] ?: 0f
                val distance = if (isDragging && currentY != null) abs(currentY - itemCenterY) else Float.MAX_VALUE

                // Smooth wide cosine dome curve (no spiky pinch, spans almost full height)
                val arcFactor = if (distance < waveRadiusPx) {
                    val normalized = distance / waveRadiusPx
                    cos((PI / 2.0) * normalized).pow(1.35).toFloat()
                } else {
                    0f
                }

                val targetTranslationX = -1f * (basePeakOffsetPx * pullFactor * arcFactor)

                val animatedOffsetX by animateFloatAsState(
                    targetValue = if (isDragging) targetTranslationX else 0f,
                    animationSpec = spring(stiffness = 900f, dampingRatio = 0.78f),
                    label = "niagara_wave_x"
                )

                val hasNotification = notificationLetters.contains(char)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            val rootY = coords.positionInRoot().y + coords.size.height / 2f
                            itemCenterYMap[index] = rootY
                        }
                        .graphicsLayer {
                            translationX = animatedOffsetX
                            // Font size & scale remain strictly 1.0f just like Niagara Launcher
                        }
                ) {
                    if (hasNotification) {
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(4.dp)
                                .background(accentColor, CircleShape)
                        )
                    }

                    Text(
                        text = char,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.55f),
                                blurRadius = 4f
                            )
                        ),
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Floating current-letter circle tucked right beside the wave peak
        if (isDragging && currentY != null) {
            val currentActiveChar = if (lastSelectedIndex in INDEX_ITEMS.indices) INDEX_ITEMS[lastSelectedIndex] else "☆"
            val badgeSizeDp = 48.dp
            val badgeSizePx = with(density) { badgeSizeDp.toPx() }

            // Align vertically with the active letter
            val activeItemY = itemCenterYMap[lastSelectedIndex] ?: currentY
            val badgeOffsetY = (activeItemY - badgeSizePx / 2f).roundToInt()

            // Sit just to the left of the curve peak (matching Niagara's tight spacing)
            val peakX = basePeakOffsetPx * pullFactor
            val badgeOffsetX = -(peakX + with(density) { 32.dp.toPx() }).roundToInt()

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(x = badgeOffsetX, y = badgeOffsetY) }
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(badgeSizeDp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = currentActiveChar,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
