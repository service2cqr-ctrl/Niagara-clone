package com.niagaraclone.launcher.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WbSunny
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

private val AppModel.firstLetter: Char
    get() = label.firstOrNull()?.uppercaseChar() ?: '#'

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenAppDrawer: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val apps by viewModel.apps.collectAsState()
    val favoritePackages by viewModel.favoritePackages.collectAsState()
    val homeLayoutOffsetY by viewModel.homeOffsetY.collectAsState()
    val widgetHeight by viewModel.widgetHeight.collectAsState()

    var isWidgetMenuOpen by remember { mutableStateOf(false) }
    var isEditingWidget by remember { mutableStateOf(false) }
    var customWidgetAdded by remember { mutableStateOf(false) }
    var appForMenu by remember { mutableStateOf<AppModel?>(null) }

    // --- STATE MANAGEMENT ---
    var isDrawerExpanded by remember { mutableStateOf(false) }
    var isIndexerVisible by remember { mutableStateOf(false) }
    var activeLetter by remember { mutableStateOf<String?>(null) }

    var weatherEnabled by remember { mutableStateOf(true) }
    var showMediaWidget by remember { mutableStateOf(true) }
    var showBattery by remember { mutableStateOf(true) }

    val favorites = remember(apps, favoritePackages) {
        apps.filter { favoritePackages.contains(it.packageName) }
    }

    val groupedApps = remember(apps) {
        apps.groupBy { it.firstLetter }.toSortedMap()
    }

    // Map each letter & 'o' section to its index in LazyColumn
    val letterIndexMap = remember(groupedApps) {
        val map = mutableMapOf<String, Int>()
        var currentIndex = 0
        groupedApps.forEach { (letter, appList) ->
            map[letter.toString()] = currentIndex
            currentIndex += 1 + appList.size
        }
        map["o"] = currentIndex
        map
    }

    val jumpToLetter: (String) -> Unit = { letter ->
        activeLetter = letter
        if (letter == "⭐") {
            isDrawerExpanded = false
            scope.launch { listState.scrollToItem(0) }
        } else {
            isDrawerExpanded = true
            val targetIdx = letterIndexMap[letter] ?: 0
            scope.launch { listState.scrollToItem(targetIdx) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(enabled = isEditingWidget) {
                isEditingWidget = false
            }
    ) {
        // --- 1. MAIN CONTENT AREA ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = homeLayoutOffsetY.dp)
                .padding(start = 24.dp, end = 48.dp, top = 48.dp)
        ) {
            if (isEditingWidget) {
                AdjustmentControlBar(
                    onReset = { viewModel.updateHomeOffsetY(0f) },
                    onDrag = { deltaY ->
                        val newOffset = (homeLayoutOffsetY + deltaY).coerceIn(-100f, 200f)
                        viewModel.updateHomeOffsetY(newOffset)
                    }
                )
            }

            // CLOCK & HEADER (HIDDEN WHEN IN DRAWER STATE)
            if (!isDrawerExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { isWidgetMenuOpen = true }
                        )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "09",
                            color = Color(0xFF4DD0E1),
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "31",
                            color = Color.White,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "Mon 3 Aug", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                        if (showBattery) {
                            Text(text = "⚡ 39%", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                        }
                        if (weatherEnabled) {
                            Text(text = "💧 30°", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (customWidgetAdded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(widgetHeight.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (isEditingWidget && customWidgetAdded) {
                    AdjustmentControlBar(
                        onReset = { viewModel.updateWidgetHeight(180f) },
                        onDrag = { deltaY ->
                            val newHeight = (widgetHeight + deltaY).coerceIn(80f, 400f)
                            viewModel.updateWidgetHeight(newHeight)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // LIST (HOME FAVORITES vs EXPANDED A-Z DRAWER WITH 'o' FOOTER)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isDrawerExpanded) {
                    // HOME STATE (AT ⭐) - PINNED FAVORITES ONLY
                    items(favorites) { app ->
                        FavoriteAppRow(
                            app = app,
                            onClick = {
                                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                intent?.let { context.startActivity(it) }
                            },
                            onLongClick = { appForMenu = app }
                        )
                    }
                } else {
                    // DRAWER STATE (PAST ⭐) - FULL A-Z LIST
                    groupedApps.forEach { (letter, appGroup) ->
                        item {
                            Text(
                                text = letter.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4DD0E1),
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(appGroup) { app ->
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

                    // FOOTER ITEM FOR 'o'
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp, bottom = 48.dp)
                        ) {
                            Text(
                                text = "Cascadia Launcher",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            DrawerFooterRow(
                                icon = Icons.Default.History,
                                title = "Recently installed",
                                onClick = { /* Opens recent apps/install history */ }
                            )

                            DrawerFooterRow(
                                icon = Icons.Default.Settings,
                                title = "Cascadia settings",
                                onClick = onOpenSettings
                            )
                        }
                    }
                }
            }
        }

        // --- 2. WAVE SIDE INDEXER ---
        WaveSideIndex(
            isIndexerVisible = isIndexerVisible,
            onTouchStart = {
                isIndexerVisible = true
                isDrawerExpanded = true
            },
            onCharSelected = { char ->
                jumpToLetter(char)
            },
            onDragEnd = { lastChar ->
                if (lastChar == "⭐") {
                    isDrawerExpanded = false
                    isIndexerVisible = false
                    activeLetter = null
                } else {
                    isDrawerExpanded = true
                    isIndexerVisible = true
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // --- 3. FLOATING ACTION BUTTON (50.dp Margins from Bottom & Right, Hidden in Drawer) ---
        if (!isDrawerExpanded) {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL)
                    context.startActivity(intent)
                },
                containerColor = Color(0xFF4DD0E1),
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 50.dp, end = 50.dp)
                    .size(56.dp)
            ) {
                Icon(imageVector = Icons.Default.Phone, contentDescription = "Phone")
            }
        }

        // --- 4. WIDGET BOTTOM SHEET MENU ---
        if (isWidgetMenuOpen) {
            ModalBottomSheet(
                onDismissRequest = { isWidgetMenuOpen = false },
                containerColor = Color(0xFF181818),
                contentColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Niagara widget",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    BottomSheetRow(
                        icon = Icons.Outlined.Schedule,
                        title = "Clock style",
                        onClick = { isWidgetMenuOpen = false }
                    )

                    BottomSheetRow(
                        icon = Icons.Outlined.CalendarToday,
                        title = "Calendar",
                        subtitle = "Show upcoming events, Show date",
                        onClick = { isWidgetMenuOpen = false }
                    )

                    BottomSheetSwitchRow(
                        icon = Icons.Outlined.WbSunny,
                        title = "Weather",
                        subtitle = "Location, Temperature units",
                        checked = weatherEnabled,
                        onCheckedChange = { weatherEnabled = it }
                    )

                    BottomSheetRow(
                        icon = Icons.Default.Add,
                        title = "Add Custom Widget",
                        onClick = {
                            customWidgetAdded = true
                            isWidgetMenuOpen = false
                        }
                    )

                    BottomSheetRow(
                        icon = Icons.Default.UnfoldMore,
                        title = "Move Widget",
                        onClick = {
                            isWidgetMenuOpen = false
                            isEditingWidget = true
                        }
                    )

                    BottomSheetSwitchRow(
                        icon = null,
                        title = "Show media widget",
                        subtitle = "While playing music",
                        checked = showMediaWidget,
                        onCheckedChange = { showMediaWidget = it }
                    )

                    BottomSheetSwitchRow(
                        icon = null,
                        title = "Show Battery Percentage",
                        checked = showBattery,
                        onCheckedChange = { showBattery = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // --- 5. APP LONG PRESS MENU ---
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
}

// --- HELPER COMPONENTS ---

@Composable
private fun DrawerFooterRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
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
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        app.icon?.let { iconDrawable ->
            Image(
                bitmap = iconDrawable.toBitmap(width = 44, height = 44).asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        } ?: Spacer(modifier = Modifier.width(48.dp))

        Text(
            text = app.label,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AdjustmentControlBar(
    onReset: () -> Unit,
    onDrag: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(Color.Black.copy(alpha = 0.85f), shape = RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.y / 2f)
                }
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onReset, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(2.5.dp)
                    .background(Color.White.copy(alpha = 0.6f), shape = RoundedCornerShape(1.dp))
            )

            Icon(
                imageVector = Icons.Default.UnfoldMore,
                contentDescription = "Move",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun BottomSheetRow(
    icon: ImageVector?,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        } ?: Spacer(modifier = Modifier.width(40.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = Color.White)
            subtitle?.let {
                Text(text = it, fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun BottomSheetSwitchRow(
    icon: ImageVector?,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        } ?: Spacer(modifier = Modifier.width(40.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = Color.White)
            subtitle?.let {
                Text(text = it, fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4DD0E1)
            )
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
