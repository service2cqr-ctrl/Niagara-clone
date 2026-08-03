package com.niagaraclone.launcher.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.components.WaveSideIndex
import kotlinx.coroutines.launch

private val AppModel.firstLetter: String
    get() = label.firstOrNull()?.uppercaseChar()?.toString() ?: "#"

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenAppDrawer: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val apps by viewModel.apps.collectAsState(initial = emptyList())
    val favoritePackages by viewModel.favoritePackages.collectAsState(initial = emptySet())

    HomeScreenContent(
        viewModel = viewModel,
        apps = apps,
        favoritePackages = favoritePackages,
        onOpenSettings = onOpenSettings
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenContent(
    viewModel: LauncherViewModel,
    apps: List<AppModel>,
    favoritePackages: Set<String>,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var appForMenu by remember { mutableStateOf<AppModel?>(null) }
    
    // Dynamic alignment for Wave Side Index (Left vs Right)
    var indexerAlignment by remember { mutableStateOf(Alignment.CenterEnd) }

    // Split favorites vs full alphabetical app list
    val favorites = remember(apps, favoritePackages) {
        apps.filter { favoritePackages.contains(it.packageName) }
    }

    // Map each letter to its exact LazyColumn position
    val letterStartIndex = remember(apps, favorites) {
        val map = mutableMapOf<String, Int>()
        // Items offset: 0=Clock, 1=Date/Battery, 2=Widget, then favorites items
        val favoritesOffset = 3 + favorites.size
        apps.forEachIndexed { index, app ->
            val letter = app.firstLetter
            if (!map.containsKey(letter)) {
                map[letter] = favoritesOffset + index
            }
        }
        map
    }

    // Indexer visibility: Shows when scrolled into the A-Z list
    val isScrolledIntoDrawer by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex >= (3 + favorites.size)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Continuous Unified Sheet (Home + App Drawer)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 96.dp, start = 20.dp, end = 40.dp)
        ) {
            // ==========================================
            // 1. HOME SCREEN SECTION
            // ==========================================

            // Clock
            item(key = "home_clock") {
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

            // Info Line
            item(key = "home_info") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text("Mon 3 Aug", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("48%", color = Color.White.copy(alpha = 0.9f), fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("💧 31°", color = Color.White.copy(alpha = 0.9f), fontSize = 18.sp)
                }
            }

            // Widget Slot
            item(key = "home_widget") {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
            }

            // Pinned Favorites
            itemsIndexed(favorites, key = { _, app -> "fav_${app.packageName}" }) { _, app ->
                AppRow(
                    app = app,
                    onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        intent?.let { context.startActivity(it) }
                    },
                    onLongClick = { appForMenu = app }
                )
            }

            // ==========================================
            // 2. CONTINUOUS APP DRAWER SECTION
            // ==========================================

            itemsIndexed(apps, key = { _, app -> app.packageName }) { _, app ->
                AppRow(
                    app = app,
                    onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        intent?.let { context.startActivity(it) }
                    },
                    onLongClick = { appForMenu = app }
                )
            }

            item(key = "action_recent") {
                Spacer(modifier = Modifier.height(24.dp))
                DrawerActionRow(
                    icon = Icons.Outlined.History,
                    label = "Recently Installed Apps",
                    onClick = {}
                )
            }

            item(key = "action_settings") {
                DrawerActionRow(
                    icon = Icons.Outlined.Settings,
                    label = "Cascadia Settings",
                    onClick = onOpenSettings
                )
            }
        }

        // ==========================================
        // DUAL EDGE TOUCH STRIPS (DRAG & SCRUB)
        // ==========================================

        // Left Edge Touch Zone
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(48.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            indexerAlignment = Alignment.CenterStart
                            scope.launch { listState.scrollToItem(3 + favorites.size) }
                        },
                        onDrag = { _, _ -> },
                        onDragEnd = { indexerAlignment = Alignment.CenterEnd }
                    )
                }
        )

        // Right Edge Touch Zone
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(48.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            indexerAlignment = Alignment.CenterEnd
                            scope.launch { listState.scrollToItem(3 + favorites.size) }
                        },
                        onDrag = { _, _ -> }
                    )
                }
        )

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

        // Active Wave Side Indexer
        AnimatedVisibility(
            visible = isScrolledIntoDrawer,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(indexerAlignment)
        ) {
            WaveSideIndex(
                onCharSelected = { char ->
                    when (char) {
                        "⭐" -> {
                            // Star Icon: Scroll back to Home View & Hide Indexer
                            scope.launch { listState.animateScrollToItem(0) }
                        }
                        "o" -> {
                            scope.launch {
                                listState.scrollToItem(listState.layoutInfo.totalItemsCount - 1)
                            }
                        }
                        else -> {
                            letterStartIndex[char]?.let { targetIndex ->
                                scope.launch { listState.scrollToItem(targetIndex) }
                            }
                        }
                    }
                },
                onDragEnd = {
                    // Always snap back to the right edge after releasing
                    indexerAlignment = Alignment.CenterEnd
                }
            )
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
private fun AppRow(
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
private fun DrawerActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(18.dp))
            Text(text = label, fontSize = 18.sp, color = Color.White)
        }
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
                TextButton(onClick = onToggleFavorite, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        color = Color(0xFF4DD0E1)
                    )
                }
                TextButton(onClick = onAppInfo, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "App info", color = Color.White)
                }
                TextButton(onClick = onUninstall, modifier = Modifier.fillMaxWidth()) {
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
