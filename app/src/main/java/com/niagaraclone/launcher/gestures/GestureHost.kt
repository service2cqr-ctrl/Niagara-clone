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
    val dragThreshold = 120f // Min drag distance to trigger actions

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = { },
                onDragEnd = {
                    val currentOffset = offsetYAnimatable.value
                    coroutineScope.launch {
                        if (currentOffset < -dragThreshold) {
                            // Dragged UP past threshold -> Smart Search
                            onTriggerAction(SwipeDirection.UP)
                        } else if (currentOffset > dragThreshold) {
                            // Dragged DOWN past threshold -> Notification Shade
                            onTriggerAction(SwipeDirection.DOWN)
                        }
                        
                        // Spring rebound back to center regardless
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
                    change.consume()
                    coroutineScope.launch {
                        // Apply smooth friction damping
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
