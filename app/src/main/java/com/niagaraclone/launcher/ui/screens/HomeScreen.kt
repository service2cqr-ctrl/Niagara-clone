package com.niagaraclone.launcher.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.components.*

private val DEFAULT_INDEX_ITEMS = listOf("⭐") + ('A'..'Z').map { it.toString() } + listOf("o")

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenAppDrawer: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenRecentApps: () -> Unit = {}
) {
    val apps by viewModel.apps.collectAsState(initial = emptyList())
    val favoritePackages by viewModel.favoritePackages.collectAsState(initial = emptySet())

    HomeScreenContent(
        apps = apps,
        favoritePackages = favoritePackages
    )
}

@Composable
fun HomeScreenContent(
    apps: List<AppModel>,
    favoritePackages: Set<String>
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Filter ONLY favorite apps for Home Screen
    val favorites = remember(apps, favoritePackages) {
        apps.filter { favoritePackages.contains(it.packageName) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 40.dp),
            contentPadding = PaddingValues(top = 56.dp, bottom = 96.dp, start = 20.dp)
        ) {
            // 1. Two-Tone Stylized Big Clock
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(
                        text = "11",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4DD0E1), // Cyan/Teal
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

            // 3. Clean Widget Area Slot (No placeholder tagline)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    // Host for Android System/Samsung Widgets will render here directly
                }
            }

            // 4. Favorites List ONLY
            items(favorites, key = { "fav_${it.packageName}" }) { app ->
                FavoriteAppRow(
                    app = app,
                    onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        intent?.let { context.startActivity(it) }
                    }
                )
            }
        }

        // 5. Floating Action Button (Phone)
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

        // 6. Wave Side Indexer (Right Edge)
        WaveSideIndex(
            modifier = Modifier.align(Alignment.CenterEnd),
            onCharSelected = { },
            onDragEnd = {}
        )
    }
}

@Composable
private fun FavoriteAppRow(
    app: AppModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
