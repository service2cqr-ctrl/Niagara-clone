package com.niagaraclone.launcher.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.AppModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    apps: List<AppModel>,
    favoritePackages: Set<String>,
    onToggleFavorite: (String) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val alphabet = ('A'..'Z').toList()

    // Separate favorites and alphabetical apps list
    val favorites = remember(apps, favoritePackages) {
        apps.filter { favoritePackages.contains(it.packageName) }
    }
    val otherApps = remember(apps, favoritePackages) {
        apps.filter { !favoritePackages.contains(it.packageName) }
    }

    val alphabetMap = remember(otherApps) {
        alphabet.associateWith { char ->
            otherApps.indexOfFirst { it.label.startsWith(char, ignoreCase = true) }
        }.filter { it.value != -1 }
    }

    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        currentTime = sdf.format(Date())
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(end = 24.dp),
            contentPadding = PaddingValues(vertical = 32.dp, horizontal = 20.dp)
        ) {
            // 1. Integrated Clock & Weather Minimalist Header
            item {
                Column(modifier = Modifier.padding(bottom = 28.dp)) {
                    Text(text = currentTime, color = Color.White, fontSize = 48.sp)
                    Text(text = "Sunny • 24°C", color = Color.Gray, fontSize = 16.sp)
                }
            }

            // 2. Favorite Apps Section
            if (favorites.isNotEmpty()) {
                item {
                    Text(
                        text = "FAVORITES",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(favorites, key = { "fav_${it.packageName}" }) { app ->
                    AppRowItem(app = app, onClick = {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        launchIntent?.let { context.startActivity(it) }
                    }, onLongClick = {
                        onToggleFavorite(app.packageName)
                    })
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "ALL APPS",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            // 3. Alphabetical App List Section
            items(otherApps, key = { it.packageName }) { app ->
                AppRowItem(app = app, onClick = {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    launchIntent?.let { context.startActivity(it) }
                }, onLongClick = {
                    onToggleFavorite(app.packageName)
                })
            }
        }

        // Wave-side alphabetical indexer ticker with haptic feedback ticks
        var lastSelectedIndex by remember { mutableStateOf(-1) }
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(24.dp)
                .fillMaxHeight()
                .padding(vertical = 64.dp)
                .pointerInput(alphabetMap) {
                    detectVerticalDragGestures { change, _ ->
                        val y = change.position.y
                        val indexFraction = (y / size.height).coerceIn(0f, 1f)
                        val targetCharIndex = (indexFraction * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
                        
                        if (targetCharIndex != lastSelectedIndex) {
                            lastSelectedIndex = targetCharIndex
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        val targetChar = alphabet[targetCharIndex]
                        alphabetMap[targetChar]?.let { listIndex ->
                            coroutineScope.launch {
                                // Offset list index by header elements count
                                listState.scrollToItem(listIndex + (if (favorites.isNotEmpty()) 4 else 2))
                            }
                        }
                    }
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            alphabet.forEach { char ->
                Text(
                    text = char.toString(),
                    color = if (alphabetMap.containsKey(char)) Color.LightGray else Color.DarkGray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun AppRowItem(app: AppModel, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = app.label,
                color = Color.White,
                fontSize = 20.sp
            )
        }
        // Notification Snippet preview line if available
        if (!app.notificationText.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = app.notificationText,
                color = Color.Gray,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}
