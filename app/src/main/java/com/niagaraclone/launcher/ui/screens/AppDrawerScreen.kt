package com.niagaraclone.launcher.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel
import kotlinx.coroutines.launch

private val AppModel.firstLetter: Char
    get() = label.firstOrNull()?.uppercaseChar() ?: '#'

/**
 * The full A-Z app list. Features a right-edge letter index, search bar filtering,
 * and long-press dialog management.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerScreen(
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit
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

    val grouped = remember(filteredApps) { filteredApps.groupBy { it.firstLetter } }
    val letters = remember(grouped) { grouped.keys.sorted() }
    val letterStartIndex = remember(filteredApps, letters) {
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
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search apps") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default
            )

            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { _, app ->
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
            }
        }

        // Right-edge letter index
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
        Text(app.label, fontSize = 18.sp, modifier = Modifier.weight(1f))
        if (isFavorite) {
            Text("★", fontSize = 14.sp)
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
        title = { Text(app.label) },
        text = {
            Column {
                TextButton(onClick = onToggleFavorite) {
                    Text(if (isFavorite) "Remove from favorites" else "Add to favorites")
                }
                TextButton(onClick = onAppInfo) { Text("App info") }
                TextButton(onClick = onUninstall) { Text("Uninstall") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
