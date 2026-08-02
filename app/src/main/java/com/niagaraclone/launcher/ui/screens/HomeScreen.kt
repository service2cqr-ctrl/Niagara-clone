package com.niagaraclone.launcher.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    apps: List<AppModel>,
    favoritePackages: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenRecentApps: () -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val favorites = remember(apps, favoritePackages) {
        apps.filter { favoritePackages.contains(it.packageName) }
    }

    val alphabetMap = remember(apps) {
        INDEX_ITEMS.associateWith { char ->
            apps.indexOfFirst { it.label.startsWith(char, ignoreCase = true) }
        }.filter { it.value != -1 }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(end = 40.dp),
            contentPadding = PaddingValues(top = 48.dp, bottom = 96.dp, start = 20.dp)
        ) {
            // 1. Digital Clock & Weather Line (⚡ 52% 💧 25°)
            item {
                Column(modifier = Modifier.padding(bottom = 20.dp)) {
                    Text(text = "06:27", color = Color(0xFF4DD0E1), fontSize = 64.sp)
                    Text(text = "⚡ 52%   💧 25°", color = Color.White, fontSize = 16.sp)
                }
            }

            // 2. Custom Embedded Widget Slot (Samsung Calendar View)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text("Calendar / Agenda Widget Area", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            }

            // 3. Media Player Widget Card
            item {
                MediaPlayerCard()
            }

            // 4. Favorites App List
            items(favorites, key = { "fav_${it.packageName}" }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            intent?.let { context.startActivity(it) }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = app.label, color = Color.White, fontSize = 20.sp)
                }
            }

            // 5. Full App Drawer (Alphabetical)
            items(apps, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            intent?.let { context.startActivity(it) }
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = app.label, color = Color.White, fontSize = 18.sp)
                }
            }

            // 6. The "o" Section: Bottom launcher options matching the app drawer tail
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenRecentApps() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Recently Installed Apps", color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp)
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSettings() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Niagara Settings", color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp)
                }
            }
        }

        // Floating Action Button (FAB)
        FloatingActionButton(
            onClick = { /* Open Phone or Search */ },
            containerColor = Color(0xFF4DD0E1),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 64.dp)
                .size(56.dp)
        ) {
            Icon(Icons.Default.Phone, contentDescription = "Action", tint = Color.Black)
        }

        // Wave Scroller on Edge
        WaveSideIndex(
            modifier = Modifier.align(Alignment.CenterEnd),
            onCharSelected = { char ->
                when (char) {
                    "⭐" -> coroutineScope.launch { listState.scrollToItem(0) }
                    "o" -> coroutineScope.launch { 
                        // Scroll directly to the bottom items
                        listState.scrollToItem(listState.layoutInfo.totalItemsCount - 1) 
                    }
                    else -> {
                        alphabetMap[char]?.let { targetIndex ->
                            coroutineScope.launch {
                                // Offset past the top widgets/header items
                                listState.scrollToItem(targetIndex + 3)
                            }
                        }
                    }
                }
            },
            onDragEnd = {}
        )
    }
}
