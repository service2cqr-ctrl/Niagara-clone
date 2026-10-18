package com.niagaraclone.launcher.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.niagaraclone.launcher.ui.theme.LocalLauncherPalette

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetStackView(
    widgetIds: List<Int>,
    widgetHeightDp: Float,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    onRemoveWidget: (Int) -> Unit,
    onAddWidgetClick: () -> Unit,
    onResizeWidgetHeight: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF4DD0E1)
) {
    val palette = LocalLauncherPalette.current
    var showWidgetOptionsDialog by remember { mutableStateOf<Int?>(null) }
    var liveHeight by remember(widgetHeightDp) { mutableFloatStateOf(widgetHeightDp) }
    val density = LocalDensity.current

    if (widgetIds.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { widgetIds.size })

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(liveHeight.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(palette.cardBackground)
                .border(1.dp, palette.borderColor, RoundedCornerShape(16.dp))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val widgetId = widgetIds[page]

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AndroidView(
                        factory = { context ->
                            val widgetInfo = appWidgetManager.getAppWidgetInfo(widgetId)
                            if (widgetInfo != null) {
                                appWidgetHost.createView(context, widgetId, widgetInfo)
                            } else {
                                android.view.View(context)
                            }
                        },
                        update = { view ->
                            view.layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        IconButton(
                            onClick = { showWidgetOptionsDialog = widgetId },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Widget Options",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (widgetIds.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(widgetIds.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 14.dp else 6.dp,
                            label = "dot_width"
                        )

                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) accentColor
                                    else Color.White.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            onResizeWidgetHeight?.invoke(liveHeight)
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            liveHeight = (liveHeight + (dragAmount / density.density)).coerceIn(100f, 450f)
                        }
                    )
                }
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Resize Stack",
                tint = palette.textSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }

    showWidgetOptionsDialog?.let { targetWidgetId ->
        AlertDialog(
            onDismissRequest = { showWidgetOptionsDialog = null },
            containerColor = palette.sheetBackground,
            title = {
                Text(text = "Widget options", color = palette.textPrimary, fontSize = 18.sp)
            },
            text = {
                Column {
                    if (widgetIds.size < 4) {
                        TextButton(
                            onClick = {
                                showWidgetOptionsDialog = null
                                onAddWidgetClick()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = accentColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Add another widget to stack", color = accentColor)
                        }
                    }

                    TextButton(
                        onClick = {
                            onRemoveWidget(targetWidgetId)
                            appWidgetHost.deleteAppWidgetId(targetWidgetId)
                            showWidgetOptionsDialog = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Remove this widget", color = Color(0xFFEF4444))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWidgetOptionsDialog = null }) {
                    Text(text = "Close", color = palette.textSecondary)
                }
            }
        )
    }
}
