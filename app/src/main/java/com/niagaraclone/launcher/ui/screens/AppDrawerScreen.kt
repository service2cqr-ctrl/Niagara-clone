@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.niagaraclone.launcher.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.AppEntry
import com.niagaraclone.launcher.ui.LauncherViewModel
import kotlinx.coroutines.launch

/**
 * The full A-Z app list. Niagara's signature move is a thin letter index down
 * the right edge that jumps the list instantly, plus a search bar that filters
 * as you type.
 */
@Composable
fun AppDrawerScreen(
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit
) {
    val apps by viewModel.visibleApps.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var appForMenu by remember { mutableStateOf<AppEntry?>(null) }

    val grouped = remember(apps) { apps.groupBy { it.firstLetter } }
    val letters = remember(grouped) { grouped.keys.sorted() }
    // index of the first item for each letter, for fast scroll-to
    val letterStartIndex = remember(apps, letters) {
        val map = mutableMapOf<Char, Int>()
        var idx = 0
        for (letter in letters) {
            map[letter] = idx
            idx += grouped[letter]?.size ?: 0
        }
        map
    }

    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search apps") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default
            )

            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                itemsIndexed(apps, key = { _, app -> app.packageName }) { _, app ->
                    AppRow(
                        app = app,
                        isFavorite = app.packageName in favorites,
                        onClick = { viewModel.launchApp(app) },
                        onLongClick = { appForMenu = app }
                    )
                }
            }
        }

        // Right-edge letter index, the defining Niagara interaction
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(28.dp)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            letters.forEach { letter ->
                Text(
                    text = letter.toString(),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .combinedClickable(onClick = {
                            letterStartIndex[letter]?.let { idx ->
                                scope.launch { listState.animateScrollToItem(idx) }
                            }
                        })
                )
            }
        }
    }

    appForMenu?.let { app ->
        AppLongPressMenu(
            app = app,
            isFavorite = app.packageName in favorites,
            onDismiss = { appForMenu = null },
            onToggleFavorite = { viewModel.toggleFavorite(app); appForMenu = null },
            onHide = { viewModel.setAppHidden(app, true); appForMenu = null },
            onAppInfo = { viewModel.openAppInfo(app); appForMenu = null },
            onUninstall = { viewModel.uninstallApp(app); appForMenu = null }
        )
    }
}

@Composable
private fun AppRow(app: AppEntry, isFavorite: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(app.label, fontSize = 18.sp, modifier = Modifier.weight(1f))
        if (isFavorite) {
            Text("★", fontSize = 14.sp)
        }
    }
}

@Composable
private fun AppLongPressMenu(
    app: AppEntry,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onHide: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label) },
        text = {
            Column {
                TextButton(onClick = onToggleFavorite) {
                    Text(if (isFavorite) "Remove from favorites" else "Add to favorites")
                }
                TextButton(onClick = onHide) { Text("Hide app") }
                TextButton(onClick = onAppInfo) { Text("App info") }
                TextButton(onClick = onUninstall) { Text("Uninstall") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
