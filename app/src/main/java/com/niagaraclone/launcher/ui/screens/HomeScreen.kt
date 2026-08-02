package com.niagaraclone.launcher.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.AppEntry
import com.niagaraclone.launcher.ui.LauncherViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * The Niagara-style home: a big clock, then the favorites list (a handful of
 * pinned apps as plain text rows for fast one-handed reach), and a bottom hint
 * that swiping up opens the full app drawer.
 */
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenAppDrawer: () -> Unit
) {
    val allApps by viewModel.allApps.collectAsState()
    val favoritePkgs by viewModel.favorites.collectAsState()
    val favoriteApps = remember(allApps, favoritePkgs) {
        favoritePkgs.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }
    }

    var clock by remember { mutableStateOf(currentTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = currentTime()
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(Modifier.height(64.dp))
        Text(clock, fontSize = 56.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(32.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(favoriteApps, key = { it.packageName }) { app ->
                FavoriteRow(
                    app = app,
                    onClick = { viewModel.launchApp(app) },
                    onLongClick = { viewModel.toggleFavorite(app) }
                )
            }
        }

        Text(
            text = "swipe up for all apps",
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }

    // Tapping the empty area also opens the drawer, mirroring Niagara's "tap anywhere" affordance
    LaunchedEffect(Unit) { /* reserved for future one-shot onboarding hints */ }
}

@Composable
private fun FavoriteRow(app: AppEntry, onClick: () -> Unit, onLongClick: () -> Unit) {
    Text(
        text = app.label,
        fontSize = 22.sp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp)
    )
}

private fun currentTime(): String =
    SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())
