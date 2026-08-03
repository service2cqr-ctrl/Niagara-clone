package com.niagaraclone.launcher.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenAppDrawer: () -> Unit = {}
) {
    val apps by viewModel.apps.collectAsState(initial = emptyList())
    val favoritePackages by viewModel.favoritePackages.collectAsState(initial = emptySet())

    HomeScreenContent(
        viewModel = viewModel,
        apps = apps,
        favoritePackages = favoritePackages,
        onOpenAppDrawer = onOpenAppDrawer
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenContent(
    viewModel: LauncherViewModel,
    apps: List<AppModel>,
    favoritePackages: Set<String>,
    onOpenAppDrawer: () -> Unit
) {
    val context = LocalContext.current
    var appForMenu by remember { mutableStateOf<AppModel?>(null) }

    // Filter ONLY favorite apps for the Home Screen
    val favorites = remember(apps, favoritePackages) {
        apps.filter { favoritePackages.contains(it.packageName) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            // Left & Right Edge Gesture Detection to switch to App Drawer
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    val touchX = change.position.x
                    val edgeThreshold = 80.dp.toPx()
                    val isLeftEdge = touchX <= edgeThreshold
                    val isRightEdge = touchX >= (size.width - edgeThreshold)

                    if ((isLeftEdge || isRightEdge) && dragAmount > 0) {
                        onOpenAppDrawer()
                    }
                }
            }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 96.dp, start = 20.dp, end = 20.dp)
        ) {
            // 1. Two-Tone Stylized Clock
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(
                        text = "11",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4DD0E1),
                        letterSpacing = (-2).sp
                    )
                    Text(
                        text = "15",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-2).sp
                    )
                }
            }

            // 2. Date + Battery + Weather Info Line
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Mon 3 Aug",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "48%",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "💧 31°",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 18.sp
                    )
                }
            }

            // 3. Custom Widget Slot Area (Clean container)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    // System/Samsung Widget views render here
                }
            }

            // 4. Favorites List ONLY
            items(favorites, key = { "fav_${it.packageName}" }) { app ->
                FavoriteAppRow(
                    app = app,
                    onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        intent?.let { context.startActivity(it) }
                    },
                    onLongClick = { appForMenu = app }
                )
            }
        }

        // Floating Action Button (Phone)
        FloatingActionButton(
            onClick = {
                val intent = context.packageManager.getLaunchIntentForPackage("com.samsung.android.dialer")
                    ?: context.packageManager.getLaunchIntentForPackage("com.google.android.dialer")
                intent?.let { context.startActivity(it) }
            },
            containerColor = Color(0xFF4DD0E1),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 24.dp)
                .size(56.dp)
        ) {
            Icon(Icons.Default.Phone, contentDescription = "Phone", tint = Color.Black)
        }
    }

    // App Long-Press Menu
    appForMenu?.let { app ->
        AppLongPressMenu(
            app = app,
            isFavorite = favoritePackages.contains(app.packageName),
            onDismiss = { appForMenu = null },
            onToggleFavorite = {
                viewModel.toggleFavorite(app.packageName)
                appForMenu = null
            },
            onAppInfo = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", app.packageName, null)
                }
                context.startActivity(intent)
                appForMenu = null
            },
            onUninstall = {
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.fromParts("package", app.packageName, null)
                }
                context.startActivity(intent)
                appForMenu = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteAppRow(
    app: AppModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        app.icon?.let { iconDrawable ->
            Image(
                bitmap = iconDrawable.toBitmap(width = 44, height = 44).asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(18.dp))
        } ?: Spacer(modifier = Modifier.width(54.dp))

        Text(
            text = app.label,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AppLongPressMenu(
    app: AppModel,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = app.label, color = Color.White) },
        text = {
            Column {
                TextButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        color = Color(0xFF4DD0E1)
                    )
                }
                TextButton(
                    onClick = onAppInfo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "App info", color = Color.White)
                }
                TextButton(
                    onClick = onUninstall,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Uninstall", color = Color(0xFFEF4444))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}
