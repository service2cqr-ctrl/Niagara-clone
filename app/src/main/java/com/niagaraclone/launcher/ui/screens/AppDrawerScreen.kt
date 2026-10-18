package com.niagaraclone.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.components.AddToCategorySheet
import com.niagaraclone.launcher.ui.components.AppCustomizationSheet
import com.niagaraclone.launcher.ui.components.EditPopUpFolderSheet
import com.niagaraclone.launcher.ui.components.INDEX_ITEMS
import com.niagaraclone.launcher.ui.components.SmartSearchSheet
import com.niagaraclone.launcher.ui.components.WaveSideIndex
import com.niagaraclone.launcher.ui.theme.LocalLauncherPalette
import kotlinx.coroutines.launch

private val AppModel.firstLetter: String
    get() = label.firstOrNull()?.uppercaseChar()?.let {
        if (it in 'A'..'Z') it.toString() else "#"
    } ?: "#"

private sealed interface DrawerListItem {
    data class Header(val letter: String) : DrawerListItem
    data class AppItem(val app: AppModel) : DrawerListItem
    object RecentlyInstalledAction : DrawerListItem
    object SettingsAction : DrawerListItem
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerScreen(
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalLauncherPalette.current
    val allApps by viewModel.apps.collectAsState()
    val hiddenPackages by viewModel.hiddenPackages.collectAsState()
    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val notificationCounts by viewModel.notificationCounts.collectAsState()
    val notificationSummaries by viewModel.notificationSummaries.collectAsState()

    val accentColor = Color(accentColorArgb)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var activeLetter by remember { mutableStateOf<String?>(null) }
    var showSmartSearchSheet by remember { mutableStateOf(false) }
    var showRecentlyInstalledDialog by remember { mutableStateOf(false) }

    var selectedAppForCustomization by remember { mutableStateOf<AppModel?>(null) }
    var selectedAppForCategory by remember { mutableStateOf<AppModel?>(null) }
    var editingFolderApp by remember { mutableStateOf<AppModel?>(null) }

    // Filter out hidden apps just like Niagara Launcher
    val visibleApps = remember(allApps, hiddenPackages) {
        allApps.filter { !hiddenPackages.contains(it.packageName) }
    }

    val notificationLetters = remember(notificationSummaries, visibleApps) {
        notificationSummaries.mapNotNull { notification ->
            visibleApps.firstOrNull { it.packageName == notification.packageName }
                ?.firstLetter
        }.toSet()
    }

    // Handle system Back gesture properly
    BackHandler {
        when {
            selectedAppForCategory != null -> selectedAppForCategory = null
            selectedAppForCustomization != null -> selectedAppForCustomization = null
            editingFolderApp != null -> editingFolderApp = null
            showSmartSearchSheet -> showSmartSearchSheet = false
            showRecentlyInstalledDialog -> showRecentlyInstalledDialog = false
            else -> onDismiss()
        }
    }

    // Build grouped items with section headers (A, B, C...)
    val drawerItems = remember(visibleApps) {
        val list = mutableListOf<DrawerListItem>()
        var currentLetter = ""

        visibleApps.forEach { app ->
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

    // Map every index item (including letters with no apps) to its closest scroll position
    val letterStartIndexMap = remember(drawerItems) {
        val exactMap = mutableMapOf<String, Int>()
        drawerItems.forEachIndexed { index, item ->
            if (item is DrawerListItem.Header) {
                exactMap[item.letter] = index
            }
        }

        val fullMap = mutableMapOf<String, Int>()
        fullMap["☆"] = 0
        fullMap["⭐"] = 0
        var lastKnownIndex = 0
        INDEX_ITEMS.forEach { char ->
            if (exactMap.containsKey(char)) {
                lastKnownIndex = exactMap[char]!!
                fullMap[char] = lastKnownIndex
            } else if (char == "∘" || char == "#") {
                fullMap[char] = (drawerItems.size - 1).coerceAtLeast(0)
            } else {
                fullMap[char] = lastKnownIndex
            }
        }
        fullMap
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.screenBackground)
    ) {
        // Full A-Z App List with Niagara-style spacing and headers
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 48.dp),
            contentPadding = PaddingValues(top = 48.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(
                items = drawerItems,
                key = { _, item ->
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            style = TextStyle(
                                shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                            ),
                            modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                        )
                    }
                    is DrawerListItem.AppItem -> {
                        AppRow(
                            app = item.app,
                            badgeCount = notificationCounts[item.app.packageName] ?: 0,
                            accentColor = accentColor,
                            palette = palette,
                            onClick = {
                                val intent = context.packageManager.getLaunchIntentForPackage(item.app.packageName)
                                intent?.let { context.startActivity(it) }
                            },
                            onLongClick = { selectedAppForCustomization = item.app }
                        )
                    }
                    is DrawerListItem.RecentlyInstalledAction -> {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Cascadia Launcher",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        DrawerActionRow(
                            icon = Icons.Outlined.History,
                            label = "Recently installed",
                            palette = palette,
                            onClick = { showRecentlyInstalledDialog = true }
                        )
                    }
                    is DrawerListItem.SettingsAction -> {
                        DrawerActionRow(
                            icon = Icons.Outlined.Settings,
                            label = "Cascadia settings",
                            palette = palette,
                            onClick = onOpenSettings
                        )
                    }
                }
            }
        }

        // Niagara-style Floating Search Button at Bottom-Right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 44.dp, end = 52.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(54.dp)
                    .clickable { showSmartSearchSheet = true }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search apps",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Wave Side Indexer (Right Edge)
        WaveSideIndex(
            isIndexerVisible = true,
            notificationLetters = notificationLetters,
            activeChar = activeLetter,
            accentColor = accentColor,
            onCharSelected = { char ->
                activeLetter = char
                if (char != "☆" && char != "⭐") {
                    letterStartIndexMap[char]?.let { targetIndex ->
                        scope.launch { listState.scrollToItem(targetIndex) }
                    }
                }
            },
            onDragEnd = { lastChar ->
                activeLetter = null
                if (lastChar == "☆" || lastChar == "⭐") {
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    // --- OVERLAYS & SHEETS ---
    if (showSmartSearchSheet) {
        SmartSearchSheet(
            viewModel = viewModel,
            apps = visibleApps,
            onDismiss = { showSmartSearchSheet = false }
        )
    }

    selectedAppForCustomization?.let { app ->
        AppCustomizationSheet(
            app = app,
            viewModel = viewModel,
            onDismiss = { selectedAppForCustomization = null },
            onOpenSettings = onOpenSettings,
            onOpenCategorySheet = {
                selectedAppForCategory = app
                selectedAppForCustomization = null
            },
            onOpenEditFolderSheet = {
                editingFolderApp = app
                selectedAppForCustomization = null
            }
        )
    }

    selectedAppForCategory?.let { app ->
        AddToCategorySheet(
            app = app,
            viewModel = viewModel,
            onDismiss = { selectedAppForCategory = null }
        )
    }

    editingFolderApp?.let { parentApp ->
        EditPopUpFolderSheet(
            parentApp = parentApp,
            viewModel = viewModel,
            allApps = visibleApps,
            onDismiss = { editingFolderApp = null }
        )
    }

    if (showRecentlyInstalledDialog) {
        val recentApps = remember { viewModel.getRecentlyInstalledApps() }

        AlertDialog(
            onDismissRequest = { showRecentlyInstalledDialog = false },
            title = { Text("Recently Installed Apps", color = palette.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                if (recentApps.isEmpty()) {
                    Text("No newly installed apps in the last 30 days.", color = palette.textSecondary)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        itemsIndexed(recentApps) { _, app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) }
                                        showRecentlyInstalledDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                app.icon?.let { icon ->
                                    Image(
                                        bitmap = icon.toBitmap(width = 40, height = 40).asImageBitmap(),
                                        contentDescription = app.label,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                Text(text = app.label, color = palette.textPrimary, fontSize = 16.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecentlyInstalledDialog = false }) {
                    Text("Close", color = accentColor)
                }
            },
            containerColor = palette.sheetBackground
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: AppModel,
    badgeCount: Int = 0,
    accentColor: Color,
    palette: com.niagaraclone.launcher.ui.theme.LauncherPalette,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        app.icon?.let { iconDrawable ->
            Image(
                bitmap = iconDrawable.toBitmap(width = 48, height = 48).asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.width(18.dp))
        } ?: Spacer(modifier = Modifier.width(52.dp))

        Text(
            text = app.label,
            fontSize = 16.sp,
            color = palette.textPrimary,
            fontWeight = FontWeight.Medium,
            style = TextStyle(
                shadow = Shadow(color = Color.Black.copy(alpha = 0.45f), blurRadius = 4f)
            ),
            modifier = Modifier.weight(1f)
        )

        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accentColor, CircleShape)
            )
        }
    }
}

@Composable
private fun DrawerActionRow(
    icon: ImageVector,
    label: String,
    palette: com.niagaraclone.launcher.ui.theme.LauncherPalette,
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
            tint = palette.textPrimary.copy(alpha = 0.85f),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = palette.textPrimary
        )
    }
}
