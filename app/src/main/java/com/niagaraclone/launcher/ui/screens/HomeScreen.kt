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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.services.MediaNotificationListener
import com.niagaraclone.launcher.services.NotificationSummaryModel
import com.niagaraclone.launcher.services.ScreenLockService
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.components.CalendarAgendaSheet
import com.niagaraclone.launcher.ui.components.CascadiaButtonMenuSheet
import com.niagaraclone.launcher.ui.components.FabActionSelectionSheet
import com.niagaraclone.launcher.ui.components.MediaPlayerCard
import com.niagaraclone.launcher.ui.components.PopUpFolderCard
import com.niagaraclone.launcher.ui.components.WidgetStackView
import com.niagaraclone.launcher.ui.components.rememberCurrentTimeState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val AppModel.firstLetter: Char
    get() = label.firstOrNull()?.uppercaseChar() ?: '#'

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

    val rawShowMediaWidget by viewModel.showMediaWidget.collectAsState()
    val mediaWidgetTheme by viewModel.mediaWidgetTheme.collectAsState()
    val isPermissionGranted by viewModel.notificationPermissionGranted.collectAsState()
    val showMediaWidget = rawShowMediaWidget && isPermissionGranted

    val weatherEnabled by viewModel.weatherEnabled.collectAsState()
    val tempUnit by viewModel.weatherTempUnit.collectAsState()
    val liveWeather by viewModel.liveWeather.collectAsState()

    val mediaState by MediaNotificationListener.mediaState.collectAsState()

    val clockStyle by viewModel.clockStyle.collectAsState()
    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val quickLockEnabled by viewModel.quickLockEnabled.collectAsState()
    val showBatteryPercentage by viewModel.showBatteryPercentage.collectAsState()
    val cascadiaButtonEnabled by viewModel.cascadiaButtonEnabled.collectAsState()

    val fabTapActionType by viewModel.fabTapActionType.collectAsState()
    val fabTapPackage by viewModel.fabTapPackage.collectAsState()
    val fabSwipeActionType by viewModel.fabSwipeActionType.collectAsState()
    val fabSwipePackage by viewModel.fabSwipePackage.collectAsState()

    val accentColor = Color(accentColorArgb)

    val notificationCounts by viewModel.notificationCounts.collectAsState()
    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
    val notificationSummaries by viewModel.notificationSummaries.collectAsState()

    var showAgendaSheet by remember { mutableStateOf(false) }
    var showFabMenuSheet by remember { mutableStateOf(false) }
    var showFabEditActionSheet by remember { mutableStateOf(false) }
    var isSwipeActionEdit by remember { mutableStateOf(false) }
    var selectedNotificationForReply by remember { mutableStateOf<NotificationSummaryModel?>(null) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadCalendarEvents()
            showAgendaSheet = true
        }
    }

    val visibleApps = remember(apps, hiddenPackages) {
        apps.filter { !hiddenPackages.contains(it.packageName) }
    }

    val assignedFabApp = remember(visibleApps, fabTapPackage) {
        visibleApps.firstOrNull { it.packageName == fabTapPackage }
    }

    val assignedFabSwipeApp = remember(visibleApps, fabSwipePackage) {
        visibleApps.firstOrNull { it.packageName == fabSwipePackage }
    }

    var isWidgetMenuOpen by remember { mutableStateOf(false) }
    var isEditingWidget by remember { mutableStateOf(false) }
    var activePopUpApp by remember { mutableStateOf<AppModel?>(null) }
    var popupYOffset by remember { mutableFloatStateOf(0f) }

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

    var isDrawerExpanded by remember { mutableStateOf(false) }

    val favorites = remember(visibleApps, favoritePackages) {
        visibleApps.filter { favoritePackages.contains(it.packageName) }
    }

    val groupedApps = remember(visibleApps) {
        visibleApps.groupBy { it.firstLetter }.toSortedMap()
    }

    val executeAction: (String, AppModel?) -> Unit = { actionType, targetApp ->
        when (actionType) {
            "SEARCH" -> onOpenSearch()
            "AGENDA" -> {
                val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    viewModel.loadCalendarEvents()
                    showAgendaSheet = true
                } else {
                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                }
            }
            "VOICE_ASSISTANT" -> {
                try { context.startActivity(Intent(Intent.ACTION_VOICE_COMMAND).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (e: Exception) {}
            }
            "LOCK" -> ScreenLockService.lockScreen()
            "APP" -> {
                targetApp?.let { app ->
                    val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    intent?.let { context.startActivity(it) }
                }
            }
            else -> {
                val intent = Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (quickLockEnabled) {
                            val isLocked = ScreenLockService.lockScreen()
                            if (!isLocked) {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        }
                    },
                    onTap = { if (isEditingWidget) isEditingWidget = false }
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
                    if (homeLayoutOffsetY > 0f) Spacer(modifier = Modifier.height(homeLayoutOffsetY.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = {}, onLongClick = { isWidgetMenuOpen = true })
                    ) {
                        ClockHeaderDisplay(
                            clockStyle = clockStyle,
                            timeHours = timeHours,
                            timeMinutes = timeMinutes,
                            currentDate = currentDate,
                            accentColor = accentColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = currentDate.ifEmpty { "Wed 5 Aug" },
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 16.sp,
                                modifier = Modifier.clickable {
                                    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        viewModel.loadCalendarEvents()
                                        showAgendaSheet = true
                                    } else {
                                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                    }
                                }
                            )
                            if (showBatteryPercentage) Text(text = batteryStatus, color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                            if (weatherEnabled) Text(text = liveTempDisplay, color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                        }

                        // --- INLINE AGENDA PREVIEW ---
                        if (upcomingEvents.isNotEmpty()) {
                            val nextEvent = upcomingEvents.first()
                            val eventTime = if (nextEvent.isAllDay) "All day" else SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(nextEvent.startTimeMillis))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    viewModel.loadCalendarEvents()
                                    showAgendaSheet = true
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(accentColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$eventTime • ${nextEvent.title}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    mediaState?.let { media ->
                        if (showMediaWidget && media.isPlaying && media.title.isNotEmpty()) {
                            MediaPlayerCard(
                                albumArt = media.albumArt?.asImageBitmap(),
                                trackTitle = media.title,
                                artistName = media.artist,
                                isPlaying = media.isPlaying,
                                widgetTheme = mediaWidgetTheme,
                                onPlayPauseToggle = { MediaNotificationListener.activeController?.transportControls?.let { if (media.isPlaying) it.pause() else it.play() } },
                                onOpenApp = { context.packageManager.getLaunchIntentForPackage(media.packageName)?.let { context.startActivity(it) } },
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
                            onRemoveWidget = { viewModel.removeWidgetId(it) },
                            onAddWidgetClick = onAddWidgetClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(favorites) { app ->
                    val appNotification = notificationSummaries.firstOrNull { it.packageName == app.packageName }
                    FavoriteAppRow(
                        app = app,
                        badgeCount = notificationCounts[app.packageName] ?: 0,
                        latestNotificationText = appNotification?.text?.ifEmpty { appNotification.title },
                        accentColor = accentColor,
                        onClick = {
                            if (appNotification != null) {
                                selectedNotificationForReply = appNotification
                            } else {
                                context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) }
                            }
                        },
                        onLongClick = { },
                        onSwipeRight = { yOffset ->
                            popupYOffset = yOffset
                            activePopUpApp = app
                        }
                    )
                }

                if (isDrawerExpanded) {
                    groupedApps.forEach { (letter, appGroup) ->
                        item { Text(text = letter.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
                        items(appGroup) { app ->
                            val appNotification = notificationSummaries.firstOrNull { it.packageName == app.packageName }
                            FavoriteAppRow(
                                app = app,
                                badgeCount = notificationCounts[app.packageName] ?: 0,
                                latestNotificationText = appNotification?.text,
                                accentColor = accentColor,
                                onClick = { context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) } },
                                onLongClick = { },
                                onSwipeRight = { yOffset ->
                                    popupYOffset = yOffset
                                    activePopUpApp = app
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- CASCADIA FAB BUTTON WITH DIRECT VERTICAL DRAG SHORTCUT ---
        if (cascadiaButtonEnabled && !isDrawerExpanded) {
            var totalDragY by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 50.dp, end = 50.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor,
                    modifier = Modifier
                        .size(56.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (totalDragY < -40f) {
                                        executeAction(fabSwipeActionType, assignedFabSwipeApp)
                                    }
                                    totalDragY = 0f
                                },
                                onDragCancel = { totalDragY = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragY += dragAmount.y
                                }
                            )
                        }
                        .combinedClickable(
                            onClick = { executeAction(fabTapActionType, assignedFabApp) },
                            onLongClick = { showFabMenuSheet = true }
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            fabTapActionType == "APP" && assignedFabApp?.icon != null -> {
                                Image(
                                    bitmap = assignedFabApp.icon!!.toBitmap(width = 36, height = 36).asImageBitmap(),
                                    contentDescription = assignedFabApp.label,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            fabTapActionType == "SEARCH" -> Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black)
                            fabTapActionType == "AGENDA" -> Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = Color.Black)
                            fabTapActionType == "VOICE_ASSISTANT" -> Icon(Icons.Outlined.Mic, contentDescription = null, tint = Color.Black)
                            fabTapActionType == "LOCK" -> Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black)
                            else -> Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Black)
                        }
                    }
                }
            }
        }

        // --- OVERLAYS & SHEETS ---
        activePopUpApp?.let { parentApp ->
            val subApps = remember(parentApp) { viewModel.getFolderSubApps(parentApp.packageName) }

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, (popupYOffset - 120f).coerceAtLeast(0f).roundToInt()),
                onDismissRequest = { activePopUpApp = null }
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    PopUpFolderCard(
                        viewModel = viewModel,
                        parentPackage = parentApp.packageName,
                        folderName = parentApp.label,
                        subApps = subApps,
                        onDismiss = { activePopUpApp = null }
                    )
                }
            }
        }

        if (showAgendaSheet) {
            CalendarAgendaSheet(
                viewModel = viewModel,
                events = upcomingEvents,
                onDismiss = { showAgendaSheet = false }
            )
        }

        if (showFabMenuSheet) {
            CascadiaButtonMenuSheet(
                viewModel = viewModel,
                onEditTapAction = {
                    isSwipeActionEdit = false
                    showFabEditActionSheet = true
                },
                onEditSwipeAction = {
                    isSwipeActionEdit = true
                    showFabEditActionSheet = true
                },
                onOpenSettings = onOpenSettings,
                onDismiss = { showFabMenuSheet = false }
            )
        }

        if (showFabEditActionSheet) {
            FabActionSelectionSheet(
                viewModel = viewModel,
                apps = visibleApps,
                isSwipeAction = isSwipeActionEdit,
                onDismiss = { showFabEditActionSheet = false }
            )
        }

        selectedNotificationForReply?.let { notification ->
            var replyText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { selectedNotificationForReply = null },
                title = { Text(text = notification.title.ifEmpty { "Notification" }, color = Color.White) },
                text = {
                    Column {
                        Text(text = notification.text, color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Quick reply...", color = Color.Gray) },
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    IconButton(onClick = {
                        context.packageManager.getLaunchIntentForPackage(notification.packageName)?.let { context.startActivity(it) }
                        selectedNotificationForReply = null
                    }) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = accentColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedNotificationForReply = null }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = Color.Black
            )
        }
    }
}

@Composable
private fun ClockHeaderDisplay(
    clockStyle: String,
    timeHours: String,
    timeMinutes: String,
    currentDate: String,
    accentColor: Color
) {
    when (clockStyle) {
        "DIGITAL_MINIMAL" -> Row(verticalAlignment = Alignment.CenterVertically) { Text(text = "$timeHours:$timeMinutes", color = Color.White, fontSize = 54.sp) }
        "DIGITAL_HEADER" -> Column {
            Text(text = currentDate.uppercase(), color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Row(verticalAlignment = Alignment.CenterVertically) { Text(text = "$timeHours:$timeMinutes", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold) }
        }
        else -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = timeHours, color = accentColor, fontSize = 72.sp, fontWeight = FontWeight.Bold)
            Text(text = timeMinutes, color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteAppRow(
    app: AppModel,
    badgeCount: Int = 0,
    latestNotificationText: String? = null,
    accentColor: Color = Color(0xFF4DD0E1),
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeRight: (Float) -> Unit
) {
    var bottomY by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates -> bottomY = coordinates.boundsInRoot().bottom }
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

        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (!latestNotificationText.isNullOrEmpty()) {
                Text(
                    text = latestNotificationText,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        if (badgeCount > 0) {
            Surface(shape = CircleShape, color = accentColor, modifier = Modifier.padding(start = 8.dp)) {
                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
                    Text(text = if (badgeCount > 99) "99+" else badgeCount.toString(), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
