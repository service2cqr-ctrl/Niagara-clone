package com.niagaraclone.launcher.gestures

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

/**
 * Wraps the home screen content and reports discrete swipe gestures once a
 * drag exceeds [thresholdDp], matching Niagara's "swipe from anywhere" model
 * rather than requiring an edge-swipe. Also reports double taps.
 */
@Composable
fun GestureHost(
    modifier: Modifier = Modifier,
    thresholdDp: Int = 56,
    onSwipe: (SwipeDirection) -> Unit,
    onDoubleTap: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { thresholdDp.dp.toPx() }

    var dragTotalX by remember { mutableStateOf(0f) }
    var dragTotalY by remember { mutableStateOf(0f) }
    var consumed by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGesturesWithDoubleTap(onDoubleTap = onDoubleTap)
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragTotalX = 0f
                        dragTotalY = 0f
                        consumed = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragTotalX += dragAmount.x
                        dragTotalY += dragAmount.y

                        if (!consumed) {
                            val absX = dragTotalX.absoluteValue
                            val absY = dragTotalY.absoluteValue
                            if (absX > thresholdPx || absY > thresholdPx) {
                                consumed = true
                                val direction = if (absX > absY) {
                                    if (dragTotalX > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
                                } else {
                                    if (dragTotalY > 0) SwipeDirection.DOWN else SwipeDirection.UP
                                }
                                onSwipe(direction)
                            }
                        }
                    }
                )
            }
    ) {
        content()
    }
}
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTapGesturesWithDoubleTap(
    onDoubleTap: () -> Unit
) {
    detectTapGestures(onDoubleTap = { onDoubleTap() })
}


