package com.niagaraclone.launcher.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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

    val letterStartIndex = remember(filteredApps) {
        val map = mutableMapOf<String, Int>()
        filteredApps.forEachIndexed { index, app ->
            val letter = app.firstLetter
            if (!map.containsKey(letter)) {
                map[letter] = index
            }
        }
        map
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 40.dp)
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

            // Full A-Z App List
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                itemsIndexed(
                    items = filteredApps,
                    key = { _, app -> app.packageName }
                ) { _, app ->
                    AppRow(
                        app = app,
                        isFavorite = favorites.contains(app.packageName),
                        onClick = {
                            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            intent?.let { context.startActivity(it) }
                        },
                        onLongClick = { appForMenu = app }
                    )
                }

                item(key = "action_recently_installed") {
                    DrawerActionRow(
                        icon = Icons.Outlined.History,
                        label = "Recently Installed Apps",
                        onClick = { /* Optional action */ }
                    )
                }

                item(key = "action_cascadia_settings") {
                    DrawerActionRow(
                        icon = Icons.Outlined.Settings,
                        label = "Cascadia Settings",
                        onClick = onOpenSettings
                    )
                }
            }
        }

        // Wave Side Indexer (Right Edge)
        WaveSideIndex(
            modifier = Modifier.align(Alignment.CenterEnd),
            onCharSelected = { char ->
                when (char) {
                    "⭐" -> {
                        // Tapping/Stopping on Star returns to Home Screen
                        onDismiss()
                    }
                    "o" -> {
                        scope.launch {
                            if (filteredApps.isNotEmpty()) {
                                listState.scrollToItem(filteredApps.size - 1)
                            }
                        }
                    }
                    else -> {
                        letterStartIndex[char]?.let { targetIndex ->
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
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
