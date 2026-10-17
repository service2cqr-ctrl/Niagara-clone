package com.niagaraclone.launcher.gestures

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class SwipeDirection { UP, DOWN }

@Composable
fun GestureHost(
    modifier: Modifier = Modifier,
    onTriggerAction: (SwipeDirection) -> Unit,
    content: @Composable (offsetY: Float) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetYAnimatable = remember { Animatable(0f) }
    val density = LocalDensity.current
    val dragThreshold = 120f
    var ignoreCurrentDrag by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.pointerInput(Unit) {
            val edgeIgnoreWidthPx = with(density) { 64.dp.toPx() }

            detectVerticalDragGestures(
                onDragStart = { startOffset ->
                    // Do not intercept vertical drags on the right alphabet index strip
                    ignoreCurrentDrag = startOffset.x >= (size.width - edgeIgnoreWidthPx)
                },
                onDragEnd = {
                    if (ignoreCurrentDrag) {
                        ignoreCurrentDrag = false
                        return@detectVerticalDragGestures
                    }
                    val currentOffset = offsetYAnimatable.value
                    coroutineScope.launch {
                        if (currentOffset < -dragThreshold) {
                            onTriggerAction(SwipeDirection.UP)
                        } else if (currentOffset > dragThreshold) {
                            onTriggerAction(SwipeDirection.DOWN)
                        }

                        offsetYAnimatable.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                },
                onDragCancel = {
                    ignoreCurrentDrag = false
                    coroutineScope.launch {
                        offsetYAnimatable.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                },
                onVerticalDrag = { change, dragAmount ->
                    if (ignoreCurrentDrag) return@detectVerticalDragGestures
                    change.consume()
                    coroutineScope.launch {
                        val newOffset = offsetYAnimatable.value + (dragAmount * 0.65f)
                        offsetYAnimatable.snapTo(newOffset)
                    }
                }
            )
        }
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                translationY = offsetYAnimatable.value
            }
        ) {
            content(offsetYAnimatable.value)
        }
    }
}
