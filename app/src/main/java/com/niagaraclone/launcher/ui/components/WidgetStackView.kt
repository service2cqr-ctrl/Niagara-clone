package com.niagaraclone.launcher.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetStackView(
    widgetIds: List<Int>,
    widgetHeightDp: Float,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    onRemoveWidget: (Int) -> Unit,
    onAddWidgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWidgetOptionsDialog by remember { mutableStateOf<Int?>(null) }

    if (widgetIds.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { widgetIds.size })

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(widgetHeightDp.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        // --- 1. SWIPEABLE WIDGET PAGER ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val widgetId = widgetIds[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showWidgetOptionsDialog = widgetId }
                    )
            ) {
                // Host Native AppWidgetHostView via AndroidView
                AndroidView(
                    factory = { context ->
                        val widgetInfo = appWidgetManager.getAppWidgetInfo(widgetId)
                        if (widgetInfo != null) {
                            appWidgetHost.createView(context, widgetId, widgetInfo)
                        } else {
                            // Fallback view if widget info fails to load
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
            }
        }

        // --- 2. PAGINATION DOTS INDICATOR (Only shown if 2+ widgets exist) ---
        if (widgetIds.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
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
                                if (isSelected) Color(0xFF4DD0E1)
                                else Color.White.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
    }

    // --- 3. WIDGET OPTIONS DIALOG (ON LONG PRESS) ---
    showWidgetOptionsDialog?.let { targetWidgetId ->
        AlertDialog(
            onDismissRequest = { showWidgetOptionsDialog = null },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text(text = "Widget Options", color = Color.White)
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
                                tint = Color(0xFF4DD0E1)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Add another widget to stack", color = Color(0xFF4DD0E1))
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
                    Text(text = "Cancel", color = Color.Gray)
                }
            }
        )
    }
}
