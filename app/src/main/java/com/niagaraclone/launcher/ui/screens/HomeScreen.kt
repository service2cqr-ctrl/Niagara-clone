package com.niagaraclone.launcher.ui.screens

import android.Manifest
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.services.AudioConnectionReceiver
import com.niagaraclone.launcher.services.MediaNotificationListener
import com.niagaraclone.launcher.services.ScreenLockService
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.components.AutoMusicPopUpCard
import com.niagaraclone.launcher.ui.components.CalendarAgendaSheet
import com.niagaraclone.launcher.ui.components.MediaPlayerCard
import com.niagaraclone.launcher.ui.components.NotificationSummarySheet
import com.niagaraclone.launcher.ui.components.PopUpFolderCard
import com.niagaraclone.launcher.ui.components.UsageBreakerDialog
import com.niagaraclone.launcher.ui.components.WaveSideIndex
import com.niagaraclone.launcher.ui.components.WeatherBottomSheet
import com.niagaraclone.launcher.ui.components.WidgetStackView
import com.niagaraclone.launcher.ui.components.rememberCurrentTimeState
import kotlin.math.roundToInt

private val AppModel.firstLetter: Char
    get() = label.firstOrNull()?.uppercaseChar() ?: '#'

private fun openNotificationAccessSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    context.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    isEditingWidgetExternal: Boolean = false,
    onAddWidgetClick: () -> Unit = {},
    onOpenAppDrawer: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onRequestSetDefaultLauncher: () -> Unit = {},
    onOpenSearch: () -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val apps by viewModel.apps.collectAsState()
    val hiddenPackages by viewModel.hiddenPackages.collectAsState()
    val favoritePackages by viewModel.favoritePackages.collectAsState()
    val homeLayoutOffsetY by viewModel.homeOffsetY.collectAsState()
    val widgetHeight by viewModel.widgetHeight.collectAsState()
    val widgetIds by viewModel.widgetIds.collectAsState()

    val batteryStatus by viewModel.batteryStatus.collectAsState()
    val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsState()
    val isBannerDismissedSession by viewModel.isBannerDismissedSession.collectAsState()

    val rawShowMediaWidget by viewModel.showMediaWidget.collectAsState()
    val mediaWidgetTheme by viewModel.mediaWidgetTheme.collectAsState()
    val isPermissionGranted by viewModel.notificationPermissionGranted.collectAsState()
    val showMediaWidget = rawShowMediaWidget && isPermissionGranted

    val weatherEnabled by viewModel.weatherEnabled.collectAsState()
    val tempUnit by viewModel.weatherTempUnit.collectAsState()
    val liveWeather by viewModel.liveWeather.collectAsState()

    val mediaState by MediaNotificationListener.mediaState.collectAsState()

    // --- CALENDAR & NOTIFICATION STATES ---
    val notificationCounts by viewModel.notificationCounts.collectAsState()
    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
    val notificationSummaries by viewModel.notificationSummaries.collectAsState()

    var showAgendaSheet by remember { mutableStateOf(false) }
    var showNotificationSummarySheet by remember { mutableStateOf(false) }
    var pendingAppToLaunch by remember { mutableStateOf<AppModel?>(null) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadCalendarEvents()
            showAgendaSheet = true
        }
    }

    // --- AUDIO CONNECTION & MUSIC LAUNCHER STATES ---
    val isAudioConnected by AudioConnectionReceiver.isAudioDeviceConnected.collectAsState()
    val selectedMusicPackages by viewModel.selectedMusicPackages.collectAsState()

    val visibleApps = remember(apps, hiddenPackages) {
        apps.filter { !hiddenPackages.contains(it.packageName) }
    }

    val musicLauncherApps = remember(visibleApps, selectedMusicPackages) {
        if (selectedMusicPackages.isNotEmpty()) {
            visibleApps.filter { selectedMusicPackages.contains(it.packageName) }
        } else {
            viewModel.getAutoDetectedMusicApps().filter { !hiddenPackages.contains(it.packageName) }
        }
    }

    var isWidgetMenuOpen by remember { mutableStateOf(false) }
    var isWeatherSheetOpen by remember { mutableStateOf(false) }
    var isEditingWidget by remember { mutableStateOf(false) }
    var appForMenu by remember { mutableStateOf<AppModel?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showRecentlyInstalledDialog by remember { mutableStateOf(false) }

    // Dynamic Pop-up Variables
    var activePopUpApp by remember { mutableStateOf<AppModel?>(null) }
    var popupYOffset by remember { mutableFloatStateOf(0f) }

    // Real-Time System Time Receiver (12-hour format)
    val (currentTime, currentDate) = rememberCurrentTimeState(timeFormatPattern = "h:mm", dateFormatPattern = "EEE d MMM")
    val timeParts = remember(currentTime) { currentTime.split(":") }
    val timeHours = timeParts.getOrNull(0) ?: "10"
    val timeMinutes = timeParts.getOrNull(1) ?: "28"

    val liveTempDisplay = remember(liveWeather, tempUnit) {
        val c = liveWeather?.temperatureCelsius ?: 28f
        val icon = liveWeather?.conditionIcon ?: "☀️"
        val deg = if (tempUnit == "CELSIUS") "${c.roundToInt()}°" else "${(c * 9 / 5 + 32).roundToInt()}°"
        "$icon $deg"
    }

    LaunchedEffect(isEditingWidgetExternal) {
        if (isEditingWidgetExternal) isEditingWidget = true
    }

    var isDrawerExpanded by remember { mutableStateOf(false) }
    var isIndexerVisible by remember { mutableStateOf(false) }
    var activeLetter by remember { mutableStateOf<String?>(null) }
    var showBattery by remember { mutableStateOf(true) }

    val favorites = remember(visibleApps, favoritePackages) {
        visibleApps.filter { favoritePackages.contains(it.packageName) }
    }

    val groupedApps = remember(visibleApps) {
        visibleApps.groupBy { it.firstLetter }.toSortedMap()
    }

    val letterIndexMap = remember(favorites, groupedApps) {
        val map = mutableMapOf<String, Int>()
        map["⭐"] = 0
        var currentIndex = favorites.size
        groupedApps.forEach { (letter, appList) ->
            map[letter.toString()] = currentIndex
            currentIndex += 1 + appList.size
        }
        map["o"] = currentIndex
        map
    }

    LaunchedEffect(activeLetter, isDrawerExpanded) {
        val letter = activeLetter ?: return@LaunchedEffect
        if (letter == "⭐") {
            listState.scrollToItem(0)
        } else if (isDrawerExpanded) {
            val targetIdx = letterIndexMap[letter] ?: 0
            listState.scrollToItem(targetIdx)
        }
    }

    val handleAppLaunch: (AppModel) -> Unit = { app ->
        val breached = viewModel.incrementAppLaunchCount(app.packageName)
        if (breached) {
            pendingAppToLaunch = app
        } else {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            intent?.let { context.startActivity(it) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        val isLocked = ScreenLockService.lockScreen()
                        if (!isLocked) {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    },
                    onTap = {
                        if (isEditingWidget) isEditingWidget = false
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 48.dp, top = 64.dp)
        ) {
            if (!isDrawerExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (homeLayoutOffsetY > 0f) {
                        Spacer(modifier = Modifier.height(homeLayoutOffsetY.dp))
                    }

                    if (isEditingWidget) {
                        AdjustmentControlBar(
                            onReset = { viewModel.updateHomeOffsetY(0f) },
                            onDrag = { deltaY ->
                                val newOffset = (homeLayoutOffsetY + deltaY).coerceIn(-100f, 200f)
                                viewModel.updateHomeOffsetY(newOffset)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { isWidgetMenuOpen = true }
                            )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = timeHours, color = Color(0xFF4DD0E1), fontSize = 72.sp, fontWeight = FontWeight.Bold)
                            Text(text = timeMinutes, color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = currentDate.ifEmpty { "Wed 5 Aug" },
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 16.sp,
                                modifier = Modifier.clickable {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.READ_CALENDAR
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPermission) {
                                        viewModel.loadCalendarEvents()
                                        showAgendaSheet = true
                                    } else {
                                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                    }
                                }
                            )
                            if (showBattery) {
                                Text(text = batteryStatus, color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                            }
                            if (weatherEnabled) {
                                Text(
                                    text = liveTempDisplay,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 16.sp,
                                    modifier = Modifier.clickable { isWeatherSheetOpen = true }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isDefaultLauncher && !isBannerDismissedSession) {
                        SwipeableDefaultLauncherBanner(
                            onSetDefault = onRequestSetDefaultLauncher,
                            onDismiss = { viewModel.dismissBannerForSession() }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // --- ONLY SHOW ACTIVE PLAYING MEDIA CARD ---
                    mediaState?.let { media ->
                        if (showMediaWidget && media.isPlaying && media.title.isNotEmpty()) {
                            MediaPlayerCard(
                                albumArt = media.albumArt?.asImageBitmap(),
                                trackTitle = media.title,
                                artistName = media.artist,
                                isPlaying = media.isPlaying,
                                widgetTheme = mediaWidgetTheme,
                                onPlayPauseToggle = {
                                    val controller = MediaNotificationListener.activeController
                                    if (media.isPlaying) controller?.transportControls?.pause() else controller?.transportControls?.play()
                                },
                                onOpenApp = {
                                    viewModel.trackAppLaunch(media.packageName)
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(media.packageName)
                                    launchIntent?.let { context.startActivity(it) }
                                },
                                onNextTrack = { MediaNotificationListener.activeController?.transportControls?.skipToNext() },
                                onPreviousTrack = { MediaNotificationListener.activeController?.transportControls?.skipToPrevious() },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    if (widgetIds.isNotEmpty()) {
                        WidgetStackView(
                            widgetIds = widgetIds,
                            widgetHeightDp = widgetHeight,
                            appWidgetHost = appWidgetHost,
                            appWidgetManager = appWidgetManager,
                            onRemoveWidget = { id -> viewModel.removeWidgetId(id) },
                            onAddWidgetClick = onAddWidgetClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // --- INLINE MUSIC APP ROW ON AUDIO CONNECT ---
                if (!isDrawerExpanded && isAudioConnected && musicLauncherApps.isNotEmpty() && mediaState?.isPlaying != true) {
                    item {
                        AutoMusicPopUpCard(
                            musicApps = musicLauncherApps,
                            onDismiss = { }
                        )
                    }
                }

                items(favorites) { app ->
                    FavoriteAppRow(
                        app = app,
                        badgeCount = notificationCounts[app.packageName] ?: 0,
                        onClick = { handleAppLaunch(app) },
                        onLongClick = { appForMenu = app },
                        onSwipeRight = { yOffset ->
                            popupYOffset = yOffset
                            activePopUpApp = app
                        }
                    )
                }

                if (isDrawerExpanded) {
                    groupedApps.forEach { (letter, appGroup) ->
                        item {
                            Text(
                                text = letter.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(appGroup) { app ->
                            FavoriteAppRow(
                                app = app,
                                badgeCount = notificationCounts[app.packageName] ?: 0,
                                onClick = { handleAppLaunch(app) },
                                onLongClick = { appForMenu = app },
                                onSwipeRight = { yOffset ->
                                    popupYOffset = yOffset
                                    activePopUpApp = app
                                }
                            )
                        }
                    }

                    // --- APP DRAWER FOOTER ---
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 96.dp)
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
                                onClick = { showRecentlyInstalledDialog = true }
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

        // --- Recently Installed Apps Dialog ---
        if (showRecentlyInstalledDialog) {
            val recentApps = remember { viewModel.getRecentlyInstalledApps() }
            AlertDialog(
                onDismissRequest = { showRecentlyInstalledDialog = false },
                title = { Text("Recently Installed Apps", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                text = {
                    if (recentApps.isEmpty()) {
                        Text("No apps installed in the last 30 days.", color = Color.Gray)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(recentApps) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            handleAppLaunch(app)
                                            showRecentlyInstalledDialog = false
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    app.icon?.let { icon ->
                                        Image(
                                            bitmap = icon.toBitmap(width = 36, height = 36).asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Text(text = app.label, color = Color.White, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRecentlyInstalledDialog = false }) {
                        Text("Close", color = Color(0xFF4DD0E1))
                    }
                },
                containerColor = Color.Black
            )
        }

        // --- Dynamic Overlay Pop-up Card with Real Operational Apps ---
        activePopUpApp?.let { parentApp ->
            val subApps = remember(visibleApps, parentApp) {
                val categoryMatch = visibleApps.filter {
                    it.packageName != parentApp.packageName &&
                    it.label.take(3).equals(parentApp.label.take(3), ignoreCase = true)
                }
                if (categoryMatch.isNotEmpty()) categoryMatch.take(4) else visibleApps.filter { it.packageName != parentApp.packageName }.take(4)
            }

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, popupYOffset.roundToInt()),
                onDismissRequest = { activePopUpApp = null }
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    PopUpFolderCard(
                        folderName = parentApp.label,
                        subApps = subApps,
                        onDismiss = { activePopUpApp = null }
                    )
                }
            }
        }

        WaveSideIndex(
            isIndexerVisible = isIndexerVisible,
            onTouchStart = { isIndexerVisible = true; isDrawerExpanded = true },
            onCharSelected = { char ->
                activeLetter = char
                isDrawerExpanded = (char != "⭐")
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

        // --- CASCADIA QUICK ACTION TRIGGER BUTTON ---
        if (!isDrawerExpanded) {
            FloatingActionButton(
                onClick = { onOpenSearch() },
                containerColor = Color(0xFF4DD0E1),
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 50.dp, end = 50.dp)
                    .size(56.dp)
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Quick Search Action")
            }
        }

        // --- OVERLAYS & SHEETS ---
        if (showAgendaSheet) {
            CalendarAgendaSheet(
                events = upcomingEvents,
                onDismiss = { showAgendaSheet = false }
            )
        }

        if (showNotificationSummarySheet) {
            NotificationSummarySheet(
                summaries = notificationSummaries,
                onDismiss = { showNotificationSummarySheet = false }
            )
        }

        pendingAppToLaunch?.let { app ->
            val count = viewModel.getAppLaunchCountToday(app.packageName)
            UsageBreakerDialog(
                appName = app.label,
                launchCount = count,
                onContinue = {
                    val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    intent?.let { context.startActivity(it) }
                    pendingAppToLaunch = null
                },
                onDismiss = { pendingAppToLaunch = null }
            )
        }

        // Widget Long Press Bottom Sheet
        if (isWidgetMenuOpen) {
            ModalBottomSheet(
                onDismissRequest = { isWidgetMenuOpen = false },
                containerColor = Color.Black,
                contentColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(text = "Niagara widget", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))
                    BottomSheetRow(icon = Icons.Outlined.Schedule, title = "Clock style", onClick = { isWidgetMenuOpen = false })
                    BottomSheetRow(
                        icon = Icons.Outlined.CalendarToday,
                        title = "Calendar Agenda",
                        subtitle = "Show upcoming events",
                        onClick = {
                            isWidgetMenuOpen = false
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CALENDAR
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                viewModel.loadCalendarEvents()
                                showAgendaSheet = true
                            } else {
                                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                            }
                        }
                    )

                    BottomSheetRow(
                        icon = Icons.Default.Notifications,
                        title = "Notification Summary",
                        subtitle = "View bundled unread notifications",
                        onClick = {
                            isWidgetMenuOpen = false
                            showNotificationSummarySheet = true
                        }
                    )

                    BottomSheetSwitchRow(
                        icon = Icons.Outlined.WbSunny,
                        title = "Weather",
                        subtitle = "Location, Temperature units",
                        checked = weatherEnabled,
                        onCheckedChange = {
                            isWidgetMenuOpen = false
                            isWeatherSheetOpen = true
                        }
                    )

                    if (widgetIds.size < 4) {
                        BottomSheetRow(
                            icon = Icons.Default.Add,
                            title = if (widgetIds.isEmpty()) "Add Custom Widget" else "Add Widget to Stack (${widgetIds.size}/4)",
                            onClick = { isWidgetMenuOpen = false; onAddWidgetClick() }
                        )
                    }

                    BottomSheetRow(icon = Icons.Default.UnfoldMore, title = "Move Widget", onClick = { isWidgetMenuOpen = false; isEditingWidget = true })
                    BottomSheetSwitchRow(
                        icon = null,
                        title = "Show media widget",
                        subtitle = "While playing music",
                        checked = showMediaWidget,
                        onCheckedChange = { checked ->
                            viewModel.setShowMediaWidget(checked)
                            if (checked && !isPermissionGranted) showPermissionDialog = true
                        }
                    )

                    BottomSheetSwitchRow(icon = null, title = "Show Battery Percentage", checked = showBattery, onCheckedChange = { showBattery = it })
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Weather Interactive Sheet
        if (isWeatherSheetOpen) {
            WeatherBottomSheet(
                viewModel = viewModel,
                onDismiss = { isWeatherSheetOpen = false }
            )
        }

        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text(text = "Notification Access Needed", color = Color.White) },
                text = { Text(text = "To display live track info and album art, Cascadia Launcher requires Notification Access in system settings.", color = Color.White.copy(alpha = 0.8f)) },
                confirmButton = {
                    TextButton(onClick = { showPermissionDialog = false; openNotificationAccessSettings(context) }) {
                        Text(text = "Open Settings", color = Color(0xFF4DD0E1))
                    }
                },
                dismissButton = { TextButton(onClick = { showPermissionDialog = false }) { Text(text = "Cancel", color = Color.Gray) } },
                containerColor = Color.Black
            )
        }

        appForMenu?.let { app ->
            AppLongPressMenu(
                app = app,
                isFavorite = favoritePackages.contains(app.packageName),
                onDismiss = { appForMenu = null },
                onToggleFavorite = { viewModel.toggleFavorite(app.packageName); appForMenu = null },
                onAppInfo = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", app.packageName, null) }
                    context.startActivity(intent)
                    appForMenu = null
                },
                onUninstall = {
                    val intent = Intent(Intent.ACTION_DELETE).apply { data = Uri.fromParts("package", app.packageName, null) }
                    context.startActivity(intent)
                    appForMenu = null
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteAppRow(
    app: AppModel,
    badgeCount: Int = 0,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeRight: (Float) -> Unit
) {
    var bottomY by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                bottomY = coordinates.boundsInRoot().bottom
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount > 30f) {
                        change.consume()
                        onSwipeRight(bottomY)
                    }
                }
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        app.icon?.let { iconDrawable ->
            Image(
                bitmap = iconDrawable.toBitmap(width = 40, height = 40).asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        } ?: Spacer(modifier = Modifier.width(44.dp))
        Text(text = app.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)

        if (badgeCount > 0) {
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = CircleShape,
                color = Color(0xFF4DD0E1),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeableDefaultLauncherBanner(
    onSetDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { if (offsetX > 200f || offsetX < -200f) onDismiss() else offsetX = 0f },
                    onDragCancel = { offsetX = 0f },
                    onDrag = { change, dragAmount -> change.consume(); offsetX += dragAmount.x }
                )
            }
            .background(Color.Transparent)
            .padding(vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Surface(shape = CircleShape, color = Color(0xFFE5A93C), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = "Home", tint = Color.Black, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "Welcome back! Set Cascadia Launcher as your home screen?", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Set as home screen",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onSetDefault() }
                )
            }
        }
    }
}

// Helpers
@Composable
private fun DrawerFooterRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}

@Composable
private fun AdjustmentControlBar(onReset: () -> Unit, onDrag: (Float) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(32.dp).background(Color.Black.copy(alpha = 0.85f), shape = RoundedCornerShape(16.dp))
            .pointerInput(Unit) { detectDragGestures { change, dragAmount -> change.consume(); onDrag(dragAmount.y / 2f) } }
            .padding(horizontal = 12.dp), contentAlignment = Alignment.Center
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onReset, modifier = Modifier.size(24.dp)) { Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White) }
            Box(modifier = Modifier.width(120.dp).height(2.5.dp).background(Color.White.copy(alpha = 0.6f), shape = RoundedCornerShape(1.dp)))
            Icon(imageVector = Icons.Default.UnfoldMore, contentDescription = "Move", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun BottomSheetRow(icon: ImageVector?, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        icon?.let { Icon(imageVector = it, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(16.dp)) } ?: Spacer(modifier = Modifier.width(40.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = Color.White)
            subtitle?.let { Text(text = it, fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f)) }
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun BottomSheetSwitchRow(icon: ImageVector?, title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        icon?.let { Icon(imageVector = it, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(16.dp)) } ?: Spacer(modifier = Modifier.width(40.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = Color.White)
            subtitle?.let { Text(text = it, fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f)) }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF4DD0E1)))
    }
}

@Composable
private fun AppLongPressMenu(app: AppModel, isFavorite: Boolean, onDismiss: () -> Unit, onToggleFavorite: () -> Unit, onAppInfo: () -> Unit, onUninstall: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = app.label, color = Color.White) },
        text = {
            Column {
                TextButton(onClick = onToggleFavorite, modifier = Modifier.fillMaxWidth()) { Text(text = if (isFavorite) "Remove from favorites" else "Add to favorites", color = Color(0xFF4DD0E1)) }
                TextButton(onClick = onAppInfo, modifier = Modifier.fillMaxWidth()) { Text(text = "App info", color = Color.White) }
                TextButton(onClick = onUninstall, modifier = Modifier.fillMaxWidth()) { Text(text = "Uninstall", color = Color(0xFFEF4444)) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(text = "Cancel", color = Color.Gray) } },
        containerColor = Color.Black
    )
}
