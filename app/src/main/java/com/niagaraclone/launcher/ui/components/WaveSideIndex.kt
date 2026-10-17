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
import androidx.compose.ui.layout.boundsInWindow
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
    allowSystemBackOnRightEdge: Boolean = false,
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
    var touchY by remember { mutableFloatStateOf(0f) }
    var pullFactor by remember { mutableFloatStateOf(1f) }
    var lastSelectedIndex by remember { mutableIntStateOf(-1) }

    val itemCenterYMap = remember { mutableStateMapOf<Int, Float>() }

    val topPaddingDp = 165.dp
    val bottomPaddingDp = 95.dp

    // Single smooth master animation for wave expansion/collapse (prevents per-item lag and freezing)
    val waveProgress by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = spring(stiffness = 750f, dampingRatio = 0.78f),
        label = "wave_master_progress"
    )

    fun getItemY(idx: Int): Float {
        return itemCenterYMap[idx] ?: run {
            val topPx = with(density) { topPaddingDp.toPx() }
            val bottomPx = with(density) { bottomPaddingDp.toPx() }
            val availableHeight = (containerHeightPx - topPx - bottomPx).coerceAtLeast(1f)
            topPx + ((idx + 0.5f) / INDEX_ITEMS.size) * availableHeight
        }
    }

    fun updateSelection(rootY: Float) {
        var closestIdx = 0
        var minDistance = Float.MAX_VALUE

        for (idx in INDEX_ITEMS.indices) {
            val dist = abs(rootY - getItemY(idx))
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
            val topBoundPx = with(density) { (topPaddingDp - 16.dp).toPx() }
            val bottomBoundPx = containerHeightPx - with(density) { (bottomPaddingDp - 16.dp).toPx() }

            if (down.position.y < topBoundPx || down.position.y > bottomBoundPx) {
                return@awaitEachGesture
            }

            val startX = down.position.x
            val startY = down.position.y
            var isSystemBackSwipe = false

            // If the full drawer is open, allow quick horizontal inward swipes to trigger Android's Back gesture
            if (allowSystemBackOnRightEdge) {
                val slopResult = withTimeoutOrNull(75L) {
                    var activateScrub = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            activateScrub = true
                            break
                        }
                        val dx = startX - change.position.x // positive when swiping left (inward)
                        val dy = abs(change.position.y - startY)

                        if (dx > with(density) { 16.dp.toPx() } && dx > dy * 1.5f) {
                            isSystemBackSwipe = true
                            break
                        }
                        if (dy > with(density) { 4.dp.toPx() }) {
                            change.consume()
                            activateScrub = true
                            break
                        }
                    }
                    activateScrub
                }
                if (isSystemBackSwipe) {
                    return@awaitEachGesture
                }
            } else {
                down.consume()
            }

            // Guaranteed cleanup in `finally` so the indexer NEVER freezes even if OS cancels the gesture
            var selectedCharOnRelease = "☆"
            try {
                isDragging = true
                touchY = startY.coerceIn(topBoundPx, bottomBoundPx)
                pullFactor = 1f
                onTouchStart()
                updateSelection(touchY)

                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    if (change.pressed) {
                        change.consume()
                        touchY = change.position.y.coerceIn(topBoundPx, bottomBoundPx)
                        val extraPullPx = (startX - change.position.x).coerceAtLeast(0f)
                        val maxExtraPullPx = with(density) { 90.dp.toPx() }
                        pullFactor = 1f + (extraPullPx / maxExtraPullPx).coerceIn(0f, 0.35f)
                        updateSelection(touchY)
                        if (lastSelectedIndex in INDEX_ITEMS.indices) {
                            selectedCharOnRelease = INDEX_ITEMS[lastSelectedIndex]
                        }
                    }
                } while (event.changes.any { it.pressed })
            } finally {
                if (lastSelectedIndex in INDEX_ITEMS.indices) {
                    selectedCharOnRelease = INDEX_ITEMS[lastSelectedIndex]
                }
                isDragging = false
                lastSelectedIndex = -1
                onDragEnd(selectedCharOnRelease)
            }
        }
    }

    val waveRadiusPx = with(density) { 240.dp.toPx() }
    val basePeakOffsetPx = with(density) { 92.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerHeightPx = coordinates.size.height.toFloat()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (allowSystemBackOnRightEdge) {
                        // When drawer is expanded, allow system back gesture from the right edge
                        hostView.systemGestureExclusionRects = emptyList()
                    } else {
                        // On Home screen, protect only the narrow alphabet strip on the right edge from triggering system back
                        val bounds = coordinates.boundsInWindow()
                        val stripWidthPx = with(density) { 36.dp.toPx() }.toInt()
                        val topPx = (bounds.top + with(density) { topPaddingDp.toPx() }).toInt()
                        val bottomPx = (bounds.bottom - with(density) { bottomPaddingDp.toPx() }).toInt()
                        val rightRect = Rect(
                            (bounds.right - stripWidthPx).toInt(),
                            topPx,
                            bounds.right.toInt(),
                            bottomPx
                        )
                        hostView.systemGestureExclusionRects = listOf(rightRect)
                    }
                }
            }
    ) {
        // Right-edge touch target for scrubbing the alphabet bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(42.dp)
                .pointerInput(allowSystemBackOnRightEdge, handleIndexPointerInput)
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = topPaddingDp, bottom = bottomPaddingDp, end = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            INDEX_ITEMS.forEachIndexed { index, char ->
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
                            if (waveProgress > 0.001f) {
                                val itemY = getItemY(index)
                                val distance = abs(touchY - itemY)
                                val arcFactor = if (distance < waveRadiusPx) {
                                    val normalized = distance / waveRadiusPx
                                    cos((PI / 2.0) * normalized).pow(1.4).toFloat()
                                } else {
                                    0f
                                }
                                translationX = -1f * (basePeakOffsetPx * pullFactor * arcFactor * waveProgress)
                            } else {
                                translationX = 0f
                            }
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

        // Smooth continuous floating letter bubble
        if (waveProgress > 0.05f && lastSelectedIndex in INDEX_ITEMS.indices) {
            val currentActiveChar = INDEX_ITEMS[lastSelectedIndex]
            val badgeSizeDp = 48.dp
            val badgeSizePx = with(density) { badgeSizeDp.toPx() }

            // Track finger Y smoothly without stepped jumping
            val badgeOffsetY = (touchY - badgeSizePx / 2f).roundToInt()
            val peakX = basePeakOffsetPx * pullFactor * waveProgress
            val badgeOffsetX = -(peakX + with(density) { 30.dp.toPx() }).roundToInt()

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(x = badgeOffsetX, y = badgeOffsetY) }
                    .graphicsLayer {
                        scaleX = waveProgress
                        scaleY = waveProgress
                        alpha = waveProgress
                    }
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
