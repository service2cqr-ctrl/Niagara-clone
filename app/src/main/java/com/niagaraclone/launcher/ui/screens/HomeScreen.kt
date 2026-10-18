package com.niagaraclone.launcher.ui.screens

import android.Manifest
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.services.AudioConnectionReceiver
import com.niagaraclone.launcher.services.MediaNotificationListener
import com.niagaraclone.launcher.services.NotificationSummaryModel
import com.niagaraclone.launcher.services.ScreenLockService
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.components.*
import com.niagaraclone.launcher.ui.theme.LocalLauncherPalette
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val AppModel.firstLetter: Char
    get() = label.firstOrNull()?.uppercaseChar()?.let {
        if (it in 'A'..'Z') it else '#'
    } ?: '#'

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
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val palette = LocalLauncherPalette.current

    val drawerListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var hasCompletedStartupCheck by remember { mutableStateOf(false) }
    var isFullScreenOnboardingBlockingHome by remember { mutableStateOf(false) }

    val apps by viewModel.apps.collectAsState()
    val hiddenPackages by viewModel.hiddenPackages.collectAsState()
    val favoritePackages by viewModel.favoritePackages.collectAsState()
    val musicPackages by viewModel.selectedMusicPackages.collectAsState()
    val homeLayoutOffsetY by viewModel.homeOffsetY.collectAsState()
    val widgetHeight by viewModel.widgetHeight.collectAsState()
    val widgetIds by viewModel.widgetIds.collectAsState()

    val batteryStatus by viewModel.batteryStatus.collectAsState()
    val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsState()

    val rawShowMediaWidget by viewModel.showMediaWidget.collectAsState()
    val mediaWidgetTheme by viewModel.mediaWidgetTheme.collectAsState()

    val weatherEnabled by viewModel.weatherEnabled.collectAsState()
    val tempUnit by viewModel.weatherTempUnit.collectAsState()
    val liveWeather by viewModel.liveWeather.collectAsState()

    val mediaState by MediaNotificationListener.mediaState.collectAsState()
    val isAudioConnected by AudioConnectionReceiver.isAudioDeviceConnected.collectAsState()
    var dismissedMusicCard by remember { mutableStateOf(false) }

    val clockStyle by viewModel.clockStyle.collectAsState()
    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val quickLockEnabled by viewModel.quickLockEnabled.collectAsState()
    val showBatteryPercentage by viewModel.showBatteryPercentage.collectAsState()
    val cascadiaButtonEnabled by viewModel.cascadiaButtonEnabled.collectAsState()

    val calendarPreviewEnabled by viewModel.repository.calendarPreviewEnabled.collectAsState()

    val fabTapActionType by viewModel.fabTapActionType.collectAsState()
    val fabTapPackage by viewModel.fabTapPackage.collectAsState()
    val fabSwipeActionType by viewModel.fabSwipeActionType.collectAsState()
    val fabSwipePackage by viewModel.fabSwipePackage.collectAsState()

    val accentColor = Color(accentColorArgb)

    val notificationCounts by viewModel.notificationCounts.collectAsState()
    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
    val notificationSummaries by viewModel.notificationSummaries.collectAsState()

    val setupPrefs = remember { context.getSharedPreferences("cascadia_home_setup_prompts", Context.MODE_PRIVATE) }
    var dismissedAccessibilityPrompt by remember { mutableStateOf(setupPrefs.getBoolean("dismiss_accessibility", false)) }
    var dismissedCustomizePrompt by remember { mutableStateOf(setupPrefs.getBoolean("dismiss_customize", false)) }
    var dismissedProductivityPrompt by remember { mutableStateOf(setupPrefs.getBoolean("dismiss_productivity", false)) }

    var showAccessibilityInfoSheet by remember { mutableStateOf(false) }
    var showEditFavoritesOverlay by remember { mutableStateOf(false) }

    var showAgendaSheet by remember { mutableStateOf(false) }
    var showWeatherSheet by remember { mutableStateOf(false) }
    var showSmartSearchSheet by remember { mutableStateOf(false) }
    var showFabMenuSheet by remember { mutableStateOf(false) }
    var showFabEditActionSheet by remember { mutableStateOf(false) }
    var isSwipeActionEdit by remember { mutableStateOf(false) }
    var showRecentlyInstalledDialog by remember { mutableStateOf(false) }
    var isWidgetMenuOpen by remember { mutableStateOf(false) }
    var isMovingHeader by remember { mutableStateOf(false) }

    var selectedAppForCustomization by remember { mutableStateOf<AppModel?>(null) }
    var selectedAppForCategory by remember { mutableStateOf<AppModel?>(null) }
    var editingFolderApp by remember { mutableStateOf<AppModel?>(null) }

    var isDrawerExpanded by remember { mutableStateOf(false) }
    var isScrubbingIndex by remember { mutableStateOf(false) }
    var activeLetter by remember { mutableStateOf<String?>(null) }
    var scrubFingerY by remember { mutableFloatStateOf(screenHeightPx * 0.45f) }
    var activeHeaderTopOffsetPx by remember { mutableIntStateOf(0) }
    var pendingScrollLetter by remember { mutableStateOf<String?>(null) }
    var activePopUpApp by remember { mutableStateOf<AppModel?>(null) }

    LaunchedEffect(isEditingWidgetExternal) {
        if (isEditingWidgetExternal) {
            isMovingHeader = true
        }
    }

    val openClockApp = {
        try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.deskclock")
                intent?.let { context.startActivity(it) }
            } catch (e2: Exception) {}
        }
    }

    val isAnyOverlayActive = isDrawerExpanded ||
            isScrubbingIndex ||
            showEditFavoritesOverlay ||
            showAccessibilityInfoSheet ||
            activePopUpApp != null ||
            isWidgetMenuOpen ||
            isMovingHeader ||
            showSmartSearchSheet ||
            showAgendaSheet ||
            showWeatherSheet ||
            showFabMenuSheet ||
            showFabEditActionSheet ||
            selectedAppForCustomization != null ||
            selectedAppForCategory != null ||
            editingFolderApp != null ||
            showRecentlyInstalledDialog

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                viewModel.checkDefaultLauncherStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = isAnyOverlayActive) {
        when {
            showEditFavoritesOverlay -> showEditFavoritesOverlay = false
            showAccessibilityInfoSheet -> showAccessibilityInfoSheet = false
            selectedAppForCategory != null -> selectedAppForCategory = null
            selectedAppForCustomization != null -> selectedAppForCustomization = null
            editingFolderApp != null -> editingFolderApp = null
            activePopUpApp != null -> activePopUpApp = null
            isWidgetMenuOpen -> isWidgetMenuOpen = false
            isMovingHeader -> isMovingHeader = false
            showSmartSearchSheet -> showSmartSearchSheet = false
            showWeatherSheet -> showWeatherSheet = false
            showAgendaSheet -> showAgendaSheet = false
            showFabEditActionSheet -> showFabEditActionSheet = false
            showFabMenuSheet -> showFabMenuSheet = false
            showRecentlyInstalledDialog -> showRecentlyInstalledDialog = false
            isDrawerExpanded || isScrubbingIndex -> {
                isDrawerExpanded = false
                isScrubbingIndex = false
                activeLetter = null
            }
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadCalendarEvents()
            showAgendaSheet = true
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.fetchLiveWeatherForCurrentMode()
        }
        showWeatherSheet = true
    }

    val openWeatherWithPermissionCheck = {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            viewModel.fetchLiveWeatherForCurrentMode()
            showWeatherSheet = true
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val visibleApps = remember(apps, hiddenPackages) {
        apps.filter { !hiddenPackages.contains(it.packageName) }
    }

    val connectedMusicApps = remember(visibleApps, musicPackages, isAudioConnected) {
        if (!isAudioConnected) emptyList()
        else visibleApps.filter { musicPackages.contains(it.packageName) }
    }

    val assignedFabApp = remember(visibleApps, fabTapPackage) {
        visibleApps.firstOrNull { it.packageName == fabTapPackage }
    }

    val assignedFabSwipeApp = remember(visibleApps, fabSwipePackage) {
        visibleApps.firstOrNull { it.packageName == fabSwipePackage }
    }

    val notificationLetters = remember(notificationSummaries, visibleApps) {
        notificationSummaries.mapNotNull { notification ->
            visibleApps.firstOrNull { it.packageName == notification.packageName }
                ?.label?.firstOrNull()?.uppercaseChar()?.toString()
        }.toSet()
    }

    var isEditingWidget by remember { mutableStateOf(false) }
    var popupYOffset by remember { mutableFloatStateOf(0f) }

    val (currentTime, currentDate) = rememberCurrentTimeState(timeFormatPattern = "h:mm", dateFormatPattern = "EEE, MMM d")
    val timeParts = remember(currentTime) { currentTime.split(":") }
    val timeHours = timeParts.getOrNull(0) ?: "10"
    val timeMinutes = timeParts.getOrNull(1) ?: "23"

    val liveTempDisplay = remember(liveWeather, tempUnit) {
        val c = liveWeather?.temperatureCelsius ?: 28f
        val icon = liveWeather?.conditionIcon ?: "☀️"
        val deg = if (tempUnit == "CELSIUS") "${c.roundToInt()}°" else "${(c * 9 / 5 + 32).roundToInt()}°"
        "$icon $deg"
    }

    val favorites = remember(visibleApps, favoritePackages) {
        visibleApps.filter { favoritePackages.contains(it.packageName) }
    }

    val groupedApps = remember(visibleApps) {
        visibleApps.groupBy { it.firstLetter }.toSortedMap()
    }

    val resolvedScrubLetter = remember(activeLetter, groupedApps) {
        val charStr = activeLetter ?: return@remember null
        if (charStr == "☆" || charStr == "⭐" || charStr == "∘") return@remember charStr
        val targetChar = charStr.firstOrNull() ?: return@remember null
        if (groupedApps.containsKey(targetChar)) {
            targetChar.toString()
        } else {
            val availableKeys = groupedApps.keys.toList()
            val preceding = availableKeys.lastOrNull { it <= targetChar } ?: availableKeys.firstOrNull()
            preceding?.toString()
        }
    }

    val letterIndexMap = remember(groupedApps) {
        val map = mutableMapOf<String, Int>()
        map["☆"] = 0
        map["⭐"] = 0
        var currentIndex = 0

        groupedApps.forEach { (letter, appList) ->
            map[letter.toString()] = currentIndex
            currentIndex += 1 + appList.size
        }

        val totalCount = currentIndex + 1

        var lastKnownIndex = 0
        INDEX_ITEMS.forEach { char ->
            if (map.containsKey(char)) {
                lastKnownIndex = map[char]!!
            } else {
                if (char == "#" || char == "∘") {
                    map[char] = (totalCount - 1).coerceAtLeast(0)
                } else {
                    map[char] = lastKnownIndex
                }
            }
        }
        map
    }

    LaunchedEffect(pendingScrollLetter, isDrawerExpanded, activeHeaderTopOffsetPx) {
        val targetChar = pendingScrollLetter ?: return@LaunchedEffect
        if (isDrawerExpanded) {
            val targetIdx = letterIndexMap[targetChar] ?: 0
            drawerListState.scrollToItem(targetIdx, scrollOffset = -activeHeaderTopOffsetPx)
        }
    }

    val executeAction: (String, AppModel?) -> Unit = { actionType, targetApp ->
        when (actionType) {
            "SEARCH" -> showSmartSearchSheet = true
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
                    intent?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(it)
                    }
                }
            }
            else -> {
                val intent = Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent)
            }
        }
    }

    val showHomeFavorites = !isDrawerExpanded && (!isScrubbingIndex || activeLetter == "☆" || activeLetter == "⭐")
    val hideUnderlyingLayers = isFullScreenOnboardingBlockingHome || showEditFavoritesOverlay

    val homeWallpaperOverlayColor = when {
        palette.isGlass -> palette.accent.copy(alpha = 0.08f)
        palette.isLight -> Color.White.copy(alpha = 0.12f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(homeWallpaperOverlayColor)
            .pointerInput(quickLockEnabled) {
                detectTapGestures(
                    onDoubleTap = {
                        if (quickLockEnabled) {
                            val isLocked = ScreenLockService.lockScreen()
                            if (!isLocked) {
                                showAccessibilityInfoSheet = true
                            }
                        }
                    },
                    onTap = { if (isEditingWidget) isEditingWidget = false }
                )
            }
    ) {
        if (!hideUnderlyingLayers) {
            // --- LAYER 1: FULL SCROLLABLE A-Z LIST ---
            if (isDrawerExpanded) {
                LazyColumn(
                    state = drawerListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .clipToBounds()
                        .padding(start = 24.dp, end = 56.dp)
                        .alpha(if (isScrubbingIndex) 0f else 1f),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    groupedApps.forEach { (letter, appGroup) ->
                        item(key = "header_$letter") {
                            Text(
                                text = letter.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)),
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(appGroup, key = { "drawer_${it.packageName}" }) { app ->
                            val appNotification = notificationSummaries.firstOrNull { it.packageName == app.packageName }
                            val hasSubApps = remember(app.packageName) { viewModel.getFolderSubApps(app.packageName).isNotEmpty() }
                            FavoriteAppRow(
                                app = app,
                                badgeCount = notificationCounts[app.packageName] ?: 0,
                                notificationSummary = appNotification,
                                hasPopUpFolder = hasSubApps,
                                accentColor = accentColor,
                                onClick = { context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) } },
                                onLongClick = { selectedAppForCustomization = app },
                                onSwipeRight = { yOffset ->
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    popupYOffset = yOffset
                                    activePopUpApp = app
                                }
                            )
                        }
                    }

                    item(key = "drawer_footer") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 40.dp)
                        ) {
                            Text(
                                text = "Cascadia Launcher",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            DrawerFooterRow(
                                icon = Icons.Default.History,
                                title = "Recently installed",
                                accentColor = accentColor,
                                onClick = { showRecentlyInstalledDialog = true }
                            )
                            DrawerFooterRow(
                                icon = Icons.Default.Settings,
                                title = "Cascadia settings",
                                accentColor = accentColor,
                                onClick = onOpenSettings
                            )
                        }
                    }
                }
            }

            // --- LAYER 2: NIAGARA ACTIVE-LETTER-ONLY VIEW AT EXACT FINGER Y POSITION ---
            if (isScrubbingIndex && !showHomeFavorites) {
                var activeGroupHeightPx by remember { mutableIntStateOf(0) }
                val minTopPx = with(density) { 16.dp.toPx() }
                val maxBottomPx = screenHeightPx - with(density) { 140.dp.toPx() }

                val calculatedTopPx = remember(scrubFingerY, activeGroupHeightPx, screenHeightPx) {
                    val rawTop = scrubFingerY - with(density) { 110.dp.toPx() }
                    val maxAllowedTop = (maxBottomPx - activeGroupHeightPx).coerceAtLeast(minTopPx)
                    rawTop.coerceIn(minTopPx, maxAllowedTop).roundToInt()
                }

                LaunchedEffect(calculatedTopPx) {
                    activeHeaderTopOffsetPx = calculatedTopPx
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .clipToBounds()
                        .padding(start = 24.dp, end = 128.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(x = 0, y = calculatedTopPx) }
                            .onGloballyPositioned { coords ->
                                activeGroupHeightPx = coords.size.height
                            },
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (resolvedScrubLetter == "∘") {
                            Text(
                                text = "Cascadia Launcher",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            DrawerFooterRow(
                                icon = Icons.Default.History,
                                title = "Recently installed",
                                accentColor = accentColor,
                                onClick = { showRecentlyInstalledDialog = true }
                            )
                            DrawerFooterRow(
                                icon = Icons.Default.Settings,
                                title = "Cascadia settings",
                                accentColor = accentColor,
                                onClick = onOpenSettings
                            )
                        } else {
                            val letterChar = resolvedScrubLetter?.firstOrNull()
                            val activeAppsForLetter = if (letterChar != null) groupedApps[letterChar].orEmpty() else emptyList()

                            if (letterChar != null) {
                                Text(
                                    text = letterChar.toString(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.55f), blurRadius = 4f)),
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            activeAppsForLetter.take(8).forEach { app ->
                                val appNotification = notificationSummaries.firstOrNull { it.packageName == app.packageName }
                                val hasSubApps = remember(app.packageName) { viewModel.getFolderSubApps(app.packageName).isNotEmpty() }
                                FavoriteAppRow(
                                    app = app,
                                    badgeCount = notificationCounts[app.packageName] ?: 0,
                                    notificationSummary = appNotification,
                                    hasPopUpFolder = hasSubApps,
                                    accentColor = accentColor,
                                    onClick = {
                                        context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) }
                                    },
                                    onLongClick = { selectedAppForCustomization = app },
                                    onSwipeRight = { yOffset ->
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        popupYOffset = yOffset
                                        activePopUpApp = app
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- LAYER 3: NIAGARA HOME SCREEN ---
            if (showHomeFavorites) {
                val showInlinePrompts = hasCompletedStartupCheck && (!isDefaultLauncher || !dismissedAccessibilityPrompt || !dismissedCustomizePrompt || !dismissedProductivityPrompt)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(start = 28.dp, end = 56.dp)
                ) {
                    val baseTopWeight = if (widgetIds.isEmpty() && !showInlinePrompts) 0.26f else 0.08f
                    Spacer(modifier = Modifier.weight(baseTopWeight))
                    if (homeLayoutOffsetY > 0f) {
                        Spacer(modifier = Modifier.height(homeLayoutOffsetY.dp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(isMovingHeader) {
                                if (isMovingHeader) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val newOffset = (homeLayoutOffsetY + dragAmount.y / 2f).coerceIn(0f, 260f)
                                        viewModel.setHomeOffsetY(newOffset)
                                    }
                                }
                            }
                            .combinedClickable(
                                onClick = { openClockApp() },
                                onLongClick = { isWidgetMenuOpen = true }
                            )
                    ) {
                        if (isMovingHeader) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = accentColor.copy(alpha = 0.9f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.UnfoldMore, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Drag vertically to position", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    IconButton(
                                        onClick = { isMovingHeader = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.Black)
                                    }
                                }
                            }
                        }

                        ClockHeaderDisplay(
                            clockStyle = clockStyle,
                            timeHours = timeHours,
                            timeMinutes = timeMinutes,
                            currentDate = currentDate,
                            accentColor = accentColor
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = currentDate.ifEmpty { "Sun, Oct 18" },
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)),
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
                            if (showBatteryPercentage) {
                                Text(
                                    text = batteryStatus,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 15.sp,
                                    style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f))
                                )
                            }
                            if (weatherEnabled) {
                                Text(
                                    text = liveTempDisplay,
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontSize = 15.sp,
                                    style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)),
                                    modifier = Modifier.clickable { openWeatherWithPermissionCheck() }
                                )
                            }
                        }

                        AnimatedVisibility(visible = calendarPreviewEnabled && upcomingEvents.isNotEmpty()) {
                            val nextEvent = upcomingEvents.firstOrNull()
                            if (nextEvent != null) {
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val activeMedia = mediaState
                    if (rawShowMediaWidget && activeMedia != null && activeMedia.isPlaying && activeMedia.title.isNotEmpty()) {
                        MediaPlayerCard(
                            albumArt = activeMedia.albumArt?.asImageBitmap(),
                            trackTitle = activeMedia.title,
                            artistName = activeMedia.artist,
                            isPlaying = activeMedia.isPlaying,
                            widgetTheme = mediaWidgetTheme,
                            accentColor = accentColor,
                            onPlayPauseToggle = {
                                MediaNotificationListener.activeController?.transportControls?.let {
                                    if (activeMedia.isPlaying) it.pause() else it.play()
                                }
                            },
                            onOpenApp = {
                                context.packageManager.getLaunchIntentForPackage(activeMedia.packageName)?.let {
                                    context.startActivity(it)
                                }
                            },
                            onNextTrack = { MediaNotificationListener.activeController?.transportControls?.skipToNext() },
                            onPreviousTrack = { MediaNotificationListener.activeController?.transportControls?.skipToPrevious() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (widgetIds.isNotEmpty()) {
                        WidgetStackView(
                            widgetIds = widgetIds.toList(),
                            widgetHeightDp = widgetHeight.toFloat(),
                            appWidgetHost = appWidgetHost,
                            appWidgetManager = appWidgetManager,
                            onRemoveWidget = { viewModel.removeWidgetId(it) },
                            onAddWidgetClick = onAddWidgetClick,
                            onResizeWidgetHeight = { newHeight -> viewModel.setWidgetHeight(newHeight.toInt()) },
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (isAudioConnected && connectedMusicApps.isNotEmpty() && !dismissedMusicCard) {
                        AutoMusicPopUpCard(
                            viewModel = viewModel,
                            musicApps = connectedMusicApps,
                            onDismiss = { dismissedMusicCard = true }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.74f, fill = true),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        if (hasCompletedStartupCheck) {
                            if (!isDefaultLauncher) {
                                item(key = "prompt_set_default") {
                                    HomeSetupPromptRow(
                                        icon = Icons.Outlined.Home,
                                        title = "Set Cascadia Launcher as your home screen?",
                                        actionText = "Set as home screen",
                                        accentColor = accentColor,
                                        onClick = onRequestSetDefaultLauncher
                                    )
                                }
                            }

                            if (!dismissedAccessibilityPrompt && ScreenLockService.instance == null) {
                                item(key = "prompt_accessibility") {
                                    HomeSetupPromptRow(
                                        icon = Icons.Outlined.Info,
                                        title = "Some features use Accessibility access when turned on in Settings",
                                        actionText = "Learn More",
                                        accentColor = accentColor,
                                        onClick = { showAccessibilityInfoSheet = true }
                                    )
                                }
                            }

                            if (!dismissedCustomizePrompt) {
                                item(key = "prompt_customize") {
                                    HomeSetupPromptRow(
                                        icon = Icons.Outlined.Brush,
                                        title = "Customize Your Home Screen",
                                        actionText = "Customize",
                                        accentColor = accentColor,
                                        onClick = {
                                            setupPrefs.edit().putBoolean("dismiss_customize", true).apply()
                                            dismissedCustomizePrompt = true
                                            onOpenSettings()
                                        }
                                    )
                                }
                            }

                            if (!dismissedProductivityPrompt) {
                                item(key = "prompt_productivity") {
                                    HomeSetupPromptRow(
                                        icon = Icons.Outlined.StarBorder,
                                        title = "Choose Your Productivity Features",
                                        actionText = "Choose",
                                        accentColor = accentColor,
                                        onClick = {
                                            setupPrefs.edit().putBoolean("dismiss_productivity", true).apply()
                                            dismissedProductivityPrompt = true
                                            onOpenSettings()
                                        }
                                    )
                                }
                            }
                        }

                        items(favorites, key = { "fav_${it.packageName}" }) { app ->
                            val appNotification = notificationSummaries.firstOrNull { it.packageName == app.packageName }
                            val hasSubApps = remember(app.packageName) { viewModel.getFolderSubApps(app.packageName).isNotEmpty() }
                            FavoriteAppRow(
                                app = app,
                                badgeCount = notificationCounts[app.packageName] ?: 0,
                                notificationSummary = appNotification,
                                hasPopUpFolder = hasSubApps,
                                accentColor = accentColor,
                                onClick = {
                                    context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) }
                                },
                                onLongClick = { selectedAppForCustomization = app },
                                onSwipeRight = { yOffset ->
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    popupYOffset = yOffset
                                    activePopUpApp = app
                                }
                            )
                        }
                    }
                }
            }

            // --- LAYER 4: RIGHT-EDGE WAVE INDEXER ---
            WaveSideIndex(
                isIndexerVisible = true,
                allowSystemBackOnRightEdge = isDrawerExpanded,
                notificationLetters = notificationLetters,
                activeChar = activeLetter,
                accentColor = accentColor,
                onTouchStart = {
                    isScrubbingIndex = true
                    isDrawerExpanded = true
                },
                onCharSelectedWithY = { char, fingerY ->
                    activeLetter = char
                    scrubFingerY = fingerY
                    isScrubbingIndex = true
                    if (char != "☆" && char != "⭐") {
                        isDrawerExpanded = true
                        pendingScrollLetter = resolvedScrubLetter ?: char
                    }
                },
                onDragEnd = { lastChar ->
                    isScrubbingIndex = false
                    if (lastChar == "☆" || lastChar == "⭐") {
                        isDrawerExpanded = false
                        activeLetter = null
                    } else {
                        isDrawerExpanded = true
                        val targetLetter = resolvedScrubLetter ?: lastChar
                        pendingScrollLetter = targetLetter
                        activeLetter = null
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // --- LAYER 5: FLOATING CASCADIA / SEARCH BUTTON (With Elastic Motion Physics on Swipe Up!) ---
            if ((cascadiaButtonEnabled || isDrawerExpanded) && !isScrubbingIndex) {
                val currentTapActionType by rememberUpdatedState(fabTapActionType)
                val currentTapApp by rememberUpdatedState(assignedFabApp)
                val currentSwipeActionType by rememberUpdatedState(fabSwipeActionType)
                val currentSwipeApp by rememberUpdatedState(assignedFabSwipeApp)
                val currentDrawerExpanded by rememberUpdatedState(isDrawerExpanded)

                var rawFabDragY by remember { mutableFloatStateOf(0f) }
                var hasTriggeredThresholdHaptic by remember { mutableStateOf(false) }

                val animatedFabOffsetY by animateFloatAsState(
                    targetValue = rawFabDragY.coerceIn(-120f, 18f),
                    animationSpec = spring(stiffness = 550f, dampingRatio = 0.55f),
                    label = "fab_swipe_bounce"
                )

                val animatedFabScale by animateFloatAsState(
                    targetValue = if (rawFabDragY < -30f) 1.18f else 1.0f,
                    animationSpec = spring(stiffness = 550f, dampingRatio = 0.55f),
                    label = "fab_swipe_scale"
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 44.dp, end = 52.dp)
                        .graphicsLayer {
                            translationY = animatedFabOffsetY
                            scaleX = animatedFabScale
                            scaleY = animatedFabScale
                        }
                ) {
                    val fabContainerColor = when {
                        isDrawerExpanded && palette.isLight -> Color.White.copy(alpha = 0.92f)
                        isDrawerExpanded && palette.isGlass -> palette.cardBackground
                        isDrawerExpanded -> Color(0xFF3F3D46).copy(alpha = 0.88f)
                        else -> accentColor
                    }

                    Surface(
                        shape = CircleShape,
                        color = fabContainerColor,
                        border = if (palette.isGlass) androidx.compose.foundation.BorderStroke(1.dp, palette.borderColor) else null,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(54.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        rawFabDragY = 0f
                                        hasTriggeredThresholdHaptic = false
                                    },
                                    onDragEnd = {
                                        val finalDrag = rawFabDragY
                                        rawFabDragY = 0f
                                        hasTriggeredThresholdHaptic = false
                                        if (finalDrag < -28f) {
                                            executeAction(currentSwipeActionType, currentSwipeApp)
                                        }
                                    },
                                    onDragCancel = {
                                        rawFabDragY = 0f
                                        hasTriggeredThresholdHaptic = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        rawFabDragY += dragAmount.y * 0.9f
                                        if (rawFabDragY < -28f && !hasTriggeredThresholdHaptic) {
                                            hasTriggeredThresholdHaptic = true
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                )
                            }
                            .combinedClickable(
                                onClick = {
                                    if (currentDrawerExpanded) {
                                        showSmartSearchSheet = true
                                    } else {
                                        executeAction(currentTapActionType, currentTapApp)
                                    }
                                },
                                onLongClick = { showFabMenuSheet = true }
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val isShowingSwipePreview = rawFabDragY < -24f && !isDrawerExpanded
                            val displayActionType = if (isShowingSwipePreview) fabSwipeActionType else fabTapActionType
                            val displayApp = if (isShowingSwipePreview) assignedFabSwipeApp else assignedFabApp

                            if (isDrawerExpanded) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (palette.isLight) Color.Black else Color.White
                                )
                            } else {
                                when {
                                    displayActionType == "APP" && displayApp?.icon != null -> {
                                        Image(
                                            bitmap = displayApp.icon!!.toBitmap(width = 36, height = 36).asImageBitmap(),
                                            contentDescription = displayApp.label,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    displayActionType == "SEARCH" -> Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black)
                                    displayActionType == "AGENDA" -> Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = Color.Black)
                                    displayActionType == "VOICE_ASSISTANT" -> Icon(Icons.Outlined.Mic, contentDescription = null, tint = Color.Black)
                                    displayActionType == "LOCK" -> Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black)
                                    else -> Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- OVERLAYS & SHEETS ---
        if (!hasCompletedStartupCheck) {
            StartupPermissionsDialog(
                viewModel = viewModel,
                onStageChanged = { shouldBlockHome ->
                    isFullScreenOnboardingBlockingHome = shouldBlockHome
                },
                onComplete = {
                    isFullScreenOnboardingBlockingHome = false
                    hasCompletedStartupCheck = true
                }
            )
        }

        if (showEditFavoritesOverlay) {
            FavoritesPickerScreen(
                apps = visibleApps,
                initialSelected = favoritePackages,
                accentColor = accentColor,
                onDone = { updatedSet ->
                    coroutineScope.launch {
                        viewModel.repository.setFavoritePackages(updatedSet)
                        showEditFavoritesOverlay = false
                    }
                }
            )
        }

        if (showAccessibilityInfoSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAccessibilityInfoSheet = false },
                containerColor = palette.sheetBackground,
                contentColor = palette.textPrimary,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Accessibility Access", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Quick Lock lets you double-tap the home screen to lock your phone.\n\nWhen turned on in Cascadia settings → Productivity → Quick Lock, this feature uses Accessibility access to turn off your screen. No data is collected.",
                        color = palette.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = {
                            setupPrefs.edit().putBoolean("dismiss_accessibility", true).apply()
                            dismissedAccessibilityPrompt = true
                            showAccessibilityInfoSheet = false
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                        },
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Understood", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            setupPrefs.edit().putBoolean("dismiss_accessibility", true).apply()
                            dismissedAccessibilityPrompt = true
                            showAccessibilityInfoSheet = false
                        }
                    ) {
                        Text("Keep disabled", color = palette.textSecondary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (isWidgetMenuOpen) {
            WidgetMenuSheet(
                viewModel = viewModel,
                onAddWidgetClick = onAddWidgetClick,
                onMoveWidgetClick = { isMovingHeader = true },
                onOpenSettings = onOpenSettings,
                onOpenEditFavorites = { showEditFavoritesOverlay = true },
                onDismiss = { isWidgetMenuOpen = false }
            )
        }

        if (showWeatherSheet) {
            WeatherBottomSheet(
                viewModel = viewModel,
                onDismiss = { showWeatherSheet = false }
            )
        }

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

        activePopUpApp?.let { parentApp ->
            val subApps = remember(parentApp) { viewModel.getFolderSubApps(parentApp.packageName) }
            val appNotifs = remember(parentApp, notificationSummaries) {
                notificationSummaries.filter { it.packageName == parentApp.packageName }
            }

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, (popupYOffset - 110f).coerceIn(100f, screenHeightPx - 360f).roundToInt()),
                onDismissRequest = { activePopUpApp = null },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 48.dp)
                ) {
                    PopUpFolderCard(
                        viewModel = viewModel,
                        parentPackage = parentApp.packageName,
                        folderName = parentApp.label,
                        subApps = subApps,
                        appNotifications = appNotifs,
                        onEditPopUp = {
                            editingFolderApp = parentApp
                            activePopUpApp = null
                        },
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
                            items(recentApps) { app ->
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
                                            bitmap = icon.toBitmap(width = 36, height = 36).asImageBitmap(),
                                            contentDescription = app.label,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                    }
                                    Text(text = app.label, color = palette.textPrimary, fontSize = 15.sp)
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
}

@Composable
private fun HomeSetupPromptRow(
    icon: ImageVector,
    title: String,
    actionText: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = accentColor,
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.55f), blurRadius = 4f))
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = actionText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.align(Alignment.End)
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
    val textShadow = Shadow(color = Color.Black.copy(alpha = 0.45f), blurRadius = 6f)
    when (clockStyle) {
        "DIGITAL_MINIMAL" -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$timeHours:$timeMinutes", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Light, style = TextStyle(shadow = textShadow))
        }
        "DIGITAL_HEADER" -> Column {
            Text(text = currentDate.uppercase(), color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "$timeHours:$timeMinutes", color = Color.White, fontSize = 60.sp, fontWeight = FontWeight.Bold, style = TextStyle(shadow = textShadow))
            }
        }
        else -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$timeHours:$timeMinutes", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Normal, style = TextStyle(shadow = textShadow))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteAppRow(
    app: AppModel,
    badgeCount: Int = 0,
    notificationSummary: NotificationSummaryModel? = null,
    hasPopUpFolder: Boolean = false,
    accentColor: Color = Color(0xFF4DD0E1),
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeRight: (Float) -> Unit
) {
    var bottomY by remember { mutableFloatStateOf(0f) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var hasTriggeredSwipe by remember { mutableStateOf(false) }

    val animatedSwipeOffsetX by animateFloatAsState(
        targetValue = dragOffsetX.coerceIn(0f, 36f),
        animationSpec = spring(stiffness = 700f, dampingRatio = 0.7f),
        label = "app_row_swipe_x"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates -> bottomY = coordinates.boundsInRoot().bottom }
            .graphicsLayer {
                translationX = animatedSwipeOffsetX
            }
            .pointerInput(app.packageName) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragOffsetX = 0f
                        hasTriggeredSwipe = false
                    },
                    onDragEnd = {
                        dragOffsetX = 0f
                        hasTriggeredSwipe = false
                    },
                    onDragCancel = {
                        dragOffsetX = 0f
                        hasTriggeredSwipe = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        dragOffsetX += dragAmount
                        if (!hasTriggeredSwipe && kotlin.math.abs(dragOffsetX) > 42f) {
                            hasTriggeredSwipe = true
                            change.consume()
                            onSwipeRight(bottomY)
                        }
                    }
                )
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.label,
                        color = Color.White,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Medium,
                        style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.45f), blurRadius = 4f))
                    )
                    if (hasPopUpFolder) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = "Pop-up folder",
                            tint = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                if (notificationSummary != null) {
                    val previewLine = if (notificationSummary.title.isNotBlank() && notificationSummary.text.isNotBlank()) {
                        "${notificationSummary.title}: ${notificationSummary.text}"
                    } else {
                        notificationSummary.text.ifEmpty { notificationSummary.title }
                    }
                    Text(
                        text = previewLine,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 3f)),
                        modifier = Modifier.clickable { onSwipeRight(bottomY) }
                    )
                }
            }

            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(8.dp)
                        .background(accentColor, CircleShape)
                        .clickable { onSwipeRight(bottomY) }
                )
            }
        }
    }
}

@Composable
private fun DrawerFooterRow(
    icon: ImageVector,
    title: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.85f),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(18.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}
