package com.niagaraclone.launcher.ui.screens

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.components.WidgetStackView
import com.niagaraclone.launcher.ui.theme.LocalLauncherPalette

@Composable
fun WidgetsScreen(
    viewModel: LauncherViewModel,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    onAddWidgetClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val palette = LocalLauncherPalette.current

    val widgetIds by viewModel.widgetIds.collectAsState()
    val widgetHeight by viewModel.widgetHeight.collectAsState()
    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val accentColor = Color(accentColorArgb)

    // Force host to listen whenever this screen is active so widgets render immediately
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                try {
                    appWidgetHost.startListening()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler { onDismiss() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = palette.screenBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (widgetIds.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No widgets added",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add Android widgets to access them quickly from your home screen page.",
                        fontSize = 14.sp,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Button(
                        onClick = onAddWidgetClick,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Add Widget", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Widgets",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                        )
                    }

                    items(
                        items = widgetIds.toList(),
                        key = { it }
                    ) { widgetId ->
                        WidgetStackView(
                            widgetIds = listOf(widgetId),
                            widgetHeightDp = widgetHeight.toFloat(),
                            appWidgetHost = appWidgetHost,
                            appWidgetManager = appWidgetManager,
                            onRemoveWidget = { viewModel.removeWidgetId(it) },
                            onAddWidgetClick = onAddWidgetClick,
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            OutlinedButton(
                                onClick = onAddWidgetClick,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = accentColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Add another widget", color = palette.textPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
