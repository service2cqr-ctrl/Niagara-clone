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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.components.WaveSideIndex
import kotlinx.coroutines.launch

private val AppModel.firstLetter: String
    get() = label.firstOrNull()?.uppercaseChar()?.toString() ?: "#"

// Helper data class to structure section headers and app items cleanly in LazyColumn
private sealed class DrawerListItem {
    data class Header(val letter: String) : DrawerListItem()
    data class AppItem(val app: AppModel) : DrawerListItem()
    object RecentlyInstalledAction : DrawerListItem()
    object SettingsAction : DrawerListItem()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerScreen(
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val allApps by viewModel.apps.collectAsState(initial = emptyList())
    val favorites by viewModel.favoritePackages.collectAsState(initial = emptySet())
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var appForMenu by remember { mutableStateOf<AppModel?>(null) }

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) {
            allApps
        } else {
            allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Build grouped items with section headers (A, B, C...)
    val drawerItems = remember(filteredApps) {
        val list = mutableListOf<DrawerListItem>()
        var currentLetter = ""

        filteredApps.forEach { app ->
            val letter = app.firstLetter
            if (letter != currentLetter) {
                currentLetter = letter
                list.add(DrawerListItem.Header(letter))
            }
            list.add(DrawerListItem.AppItem(app))
        }

        list.add(DrawerListItem.RecentlyInstalledAction)
        list.add(DrawerListItem.SettingsAction)
        list
    }

    // Map each letter to its exact scroll index in LazyColumn
    val letterStartIndexMap = remember(drawerItems) {
        val map = mutableMapOf<String, Int>()
        drawerItems.forEachIndexed { index, item ->
            if (item is DrawerListItem.Header) {
                map[item.letter] = index
            }
        }
        map
    }

    // Determine current active letter header based on visible scroll item
    val activeVisibleLetter = remember(listState.firstVisibleItemIndex, drawerItems) {
        if (drawerItems.isNotEmpty() && listState.firstVisibleItemIndex < drawerItems.size) {
            when (val currentItem = drawerItems[listState.firstVisibleItemIndex]) {
                is DrawerListItem.Header -> currentItem.letter
                is DrawerListItem.AppItem -> currentItem.app.firstLetter
                else -> ""
            }
        } else ""
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 50.dp) // Space reserved for right-side wave index
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Search apps", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.Gray
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4DD0E1),
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Box(modifier = Modifier.weight(1f)) {
                // Full A-Z App List with Headers
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    itemsIndexed(
                        items = drawerItems,
                        key = { index, item ->
                            when (item) {
                                is DrawerListItem.Header -> "header_${item.letter}"
                                is DrawerListItem.AppItem -> item.app.packageName
                                is DrawerListItem.RecentlyInstalledAction -> "action_recently_installed"
                                is DrawerListItem.SettingsAction -> "action_cascadia_settings"
                            }
                        }
                    ) { _, item ->
                        when (item) {
                            is DrawerListItem.Header -> {
                                Text(
                                    text = item.letter,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
                                )
                            }
                            is DrawerListItem.AppItem -> {
                                AppRow(
                                    app = item.app,
                                    isFavorite = favorites.contains(item.app.packageName),
                                    onClick = {
                                        val intent = context.packageManager.getLaunchIntentForPackage(item.app.packageName)
                                        intent?.let { context.startActivity(it) }
                                    },
                                    onLongClick = { appForMenu = item.app }
                                )
                            }
                            is DrawerListItem.RecentlyInstalledAction -> {
                                DrawerActionRow(
                                    icon = Icons.Outlined.History,
                                    label = "Recently Installed Apps",
                                    onClick = { /* Optional action */ }
                                )
                            }
                            is DrawerListItem.SettingsAction -> {
                                DrawerActionRow(
                                    icon = Icons.Outlined.Settings,
                                    label = "Cascadia Settings",
                                    onClick = onOpenSettings
                                )
                            }
                        }
                    }
                }

                // Active Section Circle Badge (Floats on left when scrolling)
                AnimatedVisibility(
                    visible = activeVisibleLetter.isNotEmpty() && listState.isScrollInProgress,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF4DD0E1), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeVisibleLetter,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // Wave Side Indexer (Right Edge)
        WaveSideIndex(
            modifier = Modifier.align(Alignment.CenterEnd),
            onCharSelected = { char ->
                when (char) {
                    "⭐", "★" -> {
                        // Tapping/Stopping on Star returns to Home Screen
                        onDismiss()
                    }
                    "o", "∘" -> {
                        scope.launch {
                            if (drawerItems.isNotEmpty()) {
                                listState.scrollToItem(drawerItems.size - 1)
                            }
                        }
                    }
                    else -> {
                        letterStartIndexMap[char]?.let { targetIndex ->
                            scope.launch { listState.scrollToItem(targetIndex) }
                        }
                    }
                }
            },
            onDragEnd = {}
        )
    }

    // App Long-Press Menu
    appForMenu?.let { app ->
        AppLongPressMenu(
            app = app,
            isFavorite = favorites.contains(app.packageName),
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
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
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
            fontSize = 18.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        if (isFavorite) {
            Text(
                text = "★",
                fontSize = 14.sp,
                color = Color(0xFF4DD0E1)
            )
        }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(18.dp))
            Text(
                text = label,
                fontSize = 18.sp,
                color = Color.White
            )
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
