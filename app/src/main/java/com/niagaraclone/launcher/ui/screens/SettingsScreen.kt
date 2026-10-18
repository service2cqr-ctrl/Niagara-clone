package com.niagaraclone.launcher.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.MainActivity
import com.niagaraclone.launcher.data.MediaWidgetTheme
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.components.MusicAppSelectionSheet
import com.niagaraclone.launcher.ui.components.UserGuideSheet
import com.niagaraclone.launcher.ui.components.WeatherBottomSheet
import com.niagaraclone.launcher.ui.theme.AppTheme
import com.niagaraclone.launcher.ui.theme.LocalLauncherPalette
import com.niagaraclone.launcher.utils.IconPackManager
import kotlinx.coroutines.launch

private enum class SettingsPage {
    MAIN,
    PRODUCTIVITY,
    THEMES,
    PRO,
    ADVANCED,
    BACKUP_RESTORE,
    HIDE_APPS
}

private fun openNotificationAccessSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    context.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit,
    onAddWidgetClick: () -> Unit = {},
    onMoveWidgetClick: () -> Unit = {}
) {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }
    var showUserGuideSheet by remember { mutableStateOf(false) }

    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val accentColor = Color(accentColorArgb)
    val palette = LocalLauncherPalette.current

    BackHandler {
        when (currentPage) {
            SettingsPage.MAIN -> onDismiss()
            SettingsPage.BACKUP_RESTORE -> currentPage = SettingsPage.ADVANCED
            SettingsPage.HIDE_APPS -> currentPage = SettingsPage.PRODUCTIVITY
            else -> currentPage = SettingsPage.MAIN
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = palette.screenBackground
    ) {
        when (currentPage) {
            SettingsPage.MAIN -> MainSettingsContent(
                accentColor = accentColor,
                onNavigate = { currentPage = it },
                onOpenUserGuide = { showUserGuideSheet = true },
                onDismiss = onDismiss
            )
            SettingsPage.PRODUCTIVITY -> ProductivitySettingsContent(
                viewModel = viewModel,
                accentColor = accentColor,
                onBack = { currentPage = SettingsPage.MAIN },
                onNavigateToHideApps = { currentPage = SettingsPage.HIDE_APPS },
                onAddWidgetClick = {
                    onDismiss()
                    onAddWidgetClick()
                },
                onMoveWidgetClick = {
                    onDismiss()
                    onMoveWidgetClick()
                }
            )
            SettingsPage.THEMES -> ThemesSettingsContent(
                viewModel = viewModel,
                accentColor = accentColor,
                onBack = { currentPage = SettingsPage.MAIN }
            )
            SettingsPage.PRO -> CascadiaProContent(
                accentColor = accentColor,
                onBack = { currentPage = SettingsPage.MAIN }
            )
            SettingsPage.ADVANCED -> AdvancedSettingsContent(
                accentColor = accentColor,
                onNavigateToBackup = { currentPage = SettingsPage.BACKUP_RESTORE },
                onBack = { currentPage = SettingsPage.MAIN }
            )
            SettingsPage.BACKUP_RESTORE -> BackupRestoreContent(
                viewModel = viewModel,
                accentColor = accentColor,
                onBack = { currentPage = SettingsPage.ADVANCED }
            )
            SettingsPage.HIDE_APPS -> HideAppsScreen(
                viewModel = viewModel,
                onBack = { currentPage = SettingsPage.PRODUCTIVITY }
            )
        }
    }

    if (showUserGuideSheet) {
        UserGuideSheet(viewModel = viewModel, onDismiss = { showUserGuideSheet = false })
    }
}

// -----------------------------------------------------------------------------
// 1. MAIN SETTINGS HOME
// -----------------------------------------------------------------------------
@Composable
private fun MainSettingsContent(
    accentColor: Color,
    onNavigate: (SettingsPage) -> Unit,
    onOpenUserGuide: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalLauncherPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to Home", tint = palette.textPrimary)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Cascadia settings",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Text(
                text = "Free version",
                fontSize = 15.sp,
                color = palette.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        SettingsCategoryItem(
            icon = Icons.Outlined.StarBorder,
            title = "Productivity",
            subtitle = "Stay efficient and organized",
            accentColor = accentColor,
            onClick = { onNavigate(SettingsPage.PRODUCTIVITY) }
        )

        SettingsCategoryItem(
            icon = Icons.Outlined.Palette,
            title = "Themes",
            subtitle = "Give your home screen your personal touch",
            accentColor = accentColor,
            onClick = { onNavigate(SettingsPage.THEMES) }
        )

        SettingsCategoryItem(
            icon = Icons.Outlined.AutoAwesome,
            title = "Cascadia Pro",
            subtitle = "Unlocked",
            accentColor = accentColor,
            onClick = { onNavigate(SettingsPage.PRO) }
        )

        SettingsCategoryItem(
            icon = Icons.Outlined.HelpOutline,
            title = "Help & Feedback",
            subtitle = "Learn gestures and launcher shortcuts",
            accentColor = accentColor,
            onClick = onOpenUserGuide
        )

        SettingsCategoryItem(
            icon = Icons.Outlined.MoreHoriz,
            title = "Advanced",
            subtitle = null,
            accentColor = accentColor,
            onClick = { onNavigate(SettingsPage.ADVANCED) }
        )
    }
}

// -----------------------------------------------------------------------------
// 2. PRODUCTIVITY PAGE
// -----------------------------------------------------------------------------
@Composable
private fun ProductivitySettingsContent(
    viewModel: LauncherViewModel,
    accentColor: Color,
    onBack: () -> Unit,
    onNavigateToHideApps: () -> Unit,
    onAddWidgetClick: () -> Unit,
    onMoveWidgetClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val palette = LocalLauncherPalette.current

    var showCalendarDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var isWeatherSheetOpen by remember { mutableStateOf(false) }
    var showMusicAppSheet by remember { mutableStateOf(false) }
    var showFolderInfoDialog by remember { mutableStateOf(false) }
    var showSmartSearchDialog by remember { mutableStateOf(false) }
    var showMediaThemeDialog by remember { mutableStateOf(false) }

    val rawMediaWidgetEnabled by viewModel.showMediaWidget.collectAsState()
    val selectedMediaTheme by viewModel.mediaWidgetTheme.collectAsState()
    val isPermissionGranted by viewModel.notificationPermissionGranted.collectAsState()
    val isMediaWidgetActive = rawMediaWidgetEnabled && isPermissionGranted

    val weatherEnabled by viewModel.weatherEnabled.collectAsState()
    val quickLockEnabled by viewModel.quickLockEnabled.collectAsState()
    val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsState()
    val rotationEnabled by viewModel.rotationEnabled.collectAsState()
    val showBatteryPercentage by viewModel.showBatteryPercentage.collectAsState()
    val cascadiaButtonEnabled by viewModel.cascadiaButtonEnabled.collectAsState()

    val calendarPreviewEnabled by viewModel.repository.calendarPreviewEnabled.collectAsState()
    val searchContactsEnabled by viewModel.repository.searchContactsEnabled.collectAsState()

    val quickRepliesEnabled by viewModel.quickRepliesEnabled.collectAsState()
    val usageBreakerEnabled by viewModel.usageBreakerEnabled.collectAsState()
    val notificationSummaryEnabled by viewModel.notificationSummaryEnabled.collectAsState()
    val suggestAppsEnabled by viewModel.suggestAppsEnabled.collectAsState()
    val sortByUsageEnabled by viewModel.sortByUsageEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SubPageHeader(title = "Productivity", accentColor = accentColor, onBack = onBack)

        SectionHeader("Instant Access", accentColor)
        ToggleSettingRow(
            icon = Icons.Outlined.ChatBubbleOutline,
            title = "Quick Replies",
            subtitle = "Reply right from your home screen",
            checked = quickRepliesEnabled,
            accentColor = accentColor,
            onCheckedChange = { enabled ->
                coroutineScope.launch { viewModel.repository.setQuickRepliesEnabled(enabled) }
                if (enabled && !isPermissionGranted) {
                    showPermissionDialog = true
                }
            }
        )

        ClickableSettingRow(
            icon = Icons.Outlined.CalendarToday,
            title = "Calendar Agenda",
            subtitle = "Open your calendar agenda by tapping the date",
            onClick = { showCalendarDialog = true }
        )
        ToggleSettingRow(
            icon = Icons.Outlined.EventNote,
            title = "Calendar Preview",
            subtitle = "See upcoming events on your home screen",
            checked = calendarPreviewEnabled,
            accentColor = accentColor,
            onCheckedChange = { enabled ->
                coroutineScope.launch { viewModel.repository.setCalendarPreviewEnabled(enabled) }
            }
        )

        ToggleSettingRow(
            icon = Icons.Outlined.WbSunny,
            title = "Weather Forecast",
            subtitle = "See the full forecast by tapping the current weather",
            checked = weatherEnabled,
            accentColor = accentColor,
            onRowClick = { isWeatherSheetOpen = true },
            onCheckedChange = { enabled ->
                viewModel.setWeatherEnabled(enabled)
                if (enabled) isWeatherSheetOpen = true
            }
        )

        ToggleSettingRow(
            icon = Icons.Outlined.PlayArrow,
            title = "Media Player",
            subtitle = "Control music and videos from your home screen",
            checked = isMediaWidgetActive,
            accentColor = accentColor,
            onRowClick = { showMediaThemeDialog = true },
            onCheckedChange = { checked ->
                viewModel.setShowMediaWidget(checked)
                if (checked && !isPermissionGranted) {
                    showPermissionDialog = true
                }
            }
        )

        ClickableSettingRow(
            icon = Icons.Outlined.MusicNote,
            title = "Music Apps",
            subtitle = "Show music apps when connecting headphones or speakers",
            onClick = { showMusicAppSheet = true }
        )

        SectionHeader("Digital Wellbeing", accentColor)
        ToggleSettingRow(
            icon = Icons.Outlined.Timer,
            title = "Usage Breaker",
            subtitle = "Get reminders to take breaks",
            checked = usageBreakerEnabled,
            accentColor = accentColor,
            onCheckedChange = { coroutineScope.launch { viewModel.repository.setUsageBreakerEnabled(it) } }
        )
        ToggleSettingRow(
            icon = Icons.Outlined.NotificationsPaused,
            title = "Notification Summary",
            subtitle = "Bundle notifications to reduce distractions",
            checked = notificationSummaryEnabled,
            accentColor = accentColor,
            onCheckedChange = { enabled ->
                coroutineScope.launch { viewModel.repository.setNotificationSummaryEnabled(enabled) }
                if (enabled && !isPermissionGranted) {
                    showPermissionDialog = true
                }
            }
        )

        SectionHeader("App Organization", accentColor)
        ClickableSettingRow(
            icon = Icons.Outlined.VisibilityOff,
            title = "Hide Apps",
            subtitle = "Hide apps from your app list",
            onClick = onNavigateToHideApps
        )
        ClickableSettingRow(
            icon = Icons.Outlined.Folder,
            title = "Folders & Pop-Ups",
            subtitle = "Group apps to keep your app list tidy",
            onClick = { showFolderInfoDialog = true }
        )
        ToggleSettingRow(
            icon = Icons.Outlined.AutoAwesome,
            title = "Suggest Often-Used Apps",
            subtitle = "Directly access often-used apps from your favorites",
            checked = suggestAppsEnabled,
            accentColor = accentColor,
            onCheckedChange = { coroutineScope.launch { viewModel.repository.setSuggestAppsEnabled(it) } }
        )
        ToggleSettingRow(
            icon = Icons.Outlined.Sort,
            title = "Sort All Apps List By Usage",
            subtitle = "Prioritize most-used apps for each letter",
            checked = sortByUsageEnabled,
            accentColor = accentColor,
            onCheckedChange = { coroutineScope.launch { viewModel.repository.setSortByUsageEnabled(it) } }
        )

        SectionHeader("Smart Search", accentColor)
        ClickableSettingRow(
            icon = Icons.Outlined.Search,
            title = "Smart Search",
            subtitle = "Apps, contacts, and web in one place",
            onClick = { showSmartSearchDialog = true }
        )

        SectionHeader("Shortcuts & Gestures", accentColor)
        ToggleSettingRow(
            icon = Icons.Outlined.RadioButtonChecked,
            title = "Cascadia Button",
            subtitle = "Run quick actions from your home screen",
            checked = cascadiaButtonEnabled,
            accentColor = accentColor,
            onCheckedChange = { viewModel.setCascadiaButtonEnabled(it) }
        )
        ToggleSettingRow(
            icon = Icons.Outlined.PowerSettingsNew,
            title = "Quick Lock",
            subtitle = "Double-tap empty home screen space to lock phone",
            checked = quickLockEnabled,
            accentColor = accentColor,
            onCheckedChange = { viewModel.setQuickLockEnabled(it) }
        )

        SectionHeader("Advanced", accentColor)
        ClickableSettingRow(
            icon = Icons.Outlined.AddBox,
            title = "Add Custom Widget",
            subtitle = null,
            onClick = onAddWidgetClick
        )
        ClickableSettingRow(
            icon = Icons.Outlined.SwapVert,
            title = "Move Widget",
            subtitle = null,
            onClick = onMoveWidgetClick
        )
        ToggleSettingRow(
            icon = Icons.Outlined.BatteryStd,
            title = "Show Battery Percentage",
            subtitle = "Display battery level next to date",
            checked = showBatteryPercentage,
            accentColor = accentColor,
            onCheckedChange = { viewModel.setShowBatteryPercentage(it) }
        )
        ToggleSettingRow(
            icon = Icons.Outlined.ScreenRotation,
            title = "Allow Home Screen Rotation",
            subtitle = "When device is rotated",
            checked = rotationEnabled,
            accentColor = accentColor,
            onCheckedChange = { viewModel.setRotationEnabled(it) }
        )
        ToggleSettingRow(
            icon = Icons.Outlined.Vibration,
            title = "Allow Haptic Feedback",
            subtitle = "Follow system",
            checked = hapticFeedbackEnabled,
            accentColor = accentColor,
            onCheckedChange = { viewModel.setHapticFeedbackEnabled(it) }
        )
    }

    if (isWeatherSheetOpen) {
        WeatherBottomSheet(viewModel = viewModel, onDismiss = { isWeatherSheetOpen = false })
    }
    if (showMusicAppSheet) {
        MusicAppSelectionSheet(viewModel = viewModel, onDismiss = { showMusicAppSheet = false })
    }
    if (showFolderInfoDialog) {
        AlertDialog(
            onDismissRequest = { showFolderInfoDialog = false },
            title = { Text("Folders & Pop-ups", color = palette.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Swipe right on any favorite app on your home screen to open its Pop-up folder and shortcuts.",
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showFolderInfoDialog = false }) {
                    Text("Got it", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = palette.sheetBackground
        )
    }
    if (showSmartSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSmartSearchDialog = false },
            title = { Text("Smart Search Settings", color = palette.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                ToggleSettingRow(
                    title = "Include Contacts",
                    subtitle = "Search phone contacts and dial directly",
                    checked = searchContactsEnabled,
                    accentColor = accentColor,
                    onCheckedChange = { coroutineScope.launch { viewModel.repository.setSearchContactsEnabled(it) } }
                )
            },
            confirmButton = {
                TextButton(onClick = { showSmartSearchDialog = false }) {
                    Text("Done", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = palette.sheetBackground
        )
    }
    if (showMediaThemeDialog) {
        AlertDialog(
            onDismissRequest = { showMediaThemeDialog = false },
            title = { Text("Media Player Theme", color = palette.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    MediaWidgetTheme.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setMediaWidgetTheme(theme)
                                    showMediaThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedMediaTheme == theme),
                                onClick = {
                                    viewModel.setMediaWidgetTheme(theme)
                                    showMediaThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = accentColor,
                                    unselectedColor = palette.textSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (theme) {
                                    MediaWidgetTheme.FROSTED_GLASS -> "Frosted Glass"
                                    MediaWidgetTheme.DARK -> "Dark"
                                    MediaWidgetTheme.LIGHT -> "Light"
                                    MediaWidgetTheme.MATCH_ACCENT -> "Match Accent"
                                    MediaWidgetTheme.ALBUM_ART -> "Album Art"
                                },
                                color = palette.textPrimary,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMediaThemeDialog = false }) {
                    Text("Close", color = accentColor)
                }
            },
            containerColor = palette.sheetBackground
        )
    }
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(text = "Notification Access", color = palette.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "To display live track info, notification summaries, and inline replies, allow Notification Access in system settings.",
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        openNotificationAccessSettings(context)
                    }
                ) {
                    Text(text = "Allow notification access", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(text = "Cancel", color = palette.textSecondary)
                }
            },
            containerColor = palette.sheetBackground
        )
    }
    if (showCalendarDialog) {
        AlertDialog(
            onDismissRequest = { showCalendarDialog = false },
            title = { Text("Calendar Agenda", color = palette.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Check your plans and upcoming events by tapping the date", color = palette.textSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    ToggleSettingRow(
                        title = "Show Calendar Preview",
                        subtitle = "Display next event under date",
                        checked = calendarPreviewEnabled,
                        accentColor = accentColor,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch { viewModel.repository.setCalendarPreviewEnabled(enabled) }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalendarDialog = false }) {
                    Text("Done", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = palette.sheetBackground
        )
    }
}

// -----------------------------------------------------------------------------
// 3. THEMES PAGE (Pitch Black, Light, Glass + Accent Color Picker)
// -----------------------------------------------------------------------------
@Composable
private fun ThemesSettingsContent(
    viewModel: LauncherViewModel,
    accentColor: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentAppTheme by viewModel.theme.collectAsState()
    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val selectedIconPack by viewModel.selectedIconPack.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val palette = LocalLauncherPalette.current

    var showColorPicker by remember { mutableStateOf(false) }
    var showIconPackPicker by remember { mutableStateOf(false) }

    val iconPackManager = remember { IconPackManager(context) }
    val installedIconPacks = remember { iconPackManager.getInstalledIconPacks() }

    val namedColorPresets = listOf(
        0xFFD98CE0.toInt() to "Niagara Orchid",
        0xFF4DD0E1.toInt() to "Cascadia Teal",
        0xFF3B82F6.toInt() to "Ocean Blue",
        0xFF10B981.toInt() to "Emerald Green",
        0xFFF59E0B.toInt() to "Warm Amber",
        0xFFF43F5E.toInt() to "Coral Rose",
        0xFF8B5CF6.toInt() to "Royal Violet"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SubPageHeader(title = "Themes", accentColor = accentColor, onBack = onBack)

        SectionHeader("Theme Mode", accentColor)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ThemeCardOption(
                title = "Pitch Black",
                subtitle = "AMOLED",
                isSelected = currentAppTheme == AppTheme.PITCH_BLACK,
                backgroundColor = Color.Black,
                textColor = Color.White,
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                onClick = { coroutineScope.launch { viewModel.setAppTheme(AppTheme.PITCH_BLACK) } }
            )
            ThemeCardOption(
                title = "Light",
                subtitle = "Daylight",
                isSelected = currentAppTheme == AppTheme.LIGHT,
                backgroundColor = Color(0xFFF5F5F8),
                textColor = Color(0xFF121216),
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                onClick = { coroutineScope.launch { viewModel.setAppTheme(AppTheme.LIGHT) } }
            )
            ThemeCardOption(
                title = "Glass",
                subtitle = "Frosted",
                isSelected = currentAppTheme == AppTheme.GLASS,
                backgroundColor = accentColor.copy(alpha = 0.28f),
                textColor = Color.White,
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                onClick = { coroutineScope.launch { viewModel.setAppTheme(AppTheme.GLASS) } }
            )
        }

        SectionHeader("Personalization", accentColor)
        ClickableSettingRow(
            icon = Icons.Outlined.Palette,
            title = "Accent Color",
            subtitle = "Customize clock, wave, and theme highlights",
            onClick = { showColorPicker = true }
        )
        ClickableSettingRow(
            icon = Icons.Outlined.AutoFixHigh,
            title = "Extract Wallpaper Accent",
            subtitle = "Match accent color with your current wallpaper",
            onClick = {
                viewModel.extractWallpaperAccentColor()
                Toast.makeText(context, "Wallpaper accent color applied", Toast.LENGTH_SHORT).show()
            }
        )
        ClickableSettingRow(
            icon = Icons.Outlined.GridOn,
            title = "Icon Pack",
            subtitle = if (selectedIconPack.isEmpty() || selectedIconPack == "SYSTEM") "Default system icons" else "Custom icon pack active",
            onClick = { showIconPackPicker = true }
        )
    }

    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Choose Accent Color", color = palette.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    namedColorPresets.forEach { (colorInt, label) ->
                        val itemColor = Color(colorInt)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAccentColor(colorInt)
                                    showColorPicker = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(itemColor, CircleShape)
                                    .border(1.dp, palette.borderColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = label,
                                color = palette.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            if (accentColor.toArgb() == colorInt) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = itemColor)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) {
                    Text("Close", color = accentColor)
                }
            },
            containerColor = palette.sheetBackground
        )
    }

    if (showIconPackPicker) {
        AlertDialog(
            onDismissRequest = { showIconPackPicker = false },
            title = { Text("Select Icon Pack", color = palette.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setSelectedIconPack("SYSTEM")
                                showIconPackPicker = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedIconPack == "SYSTEM" || selectedIconPack.isEmpty(),
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Default System Icons", color = palette.textPrimary, fontSize = 15.sp)
                    }
                    installedIconPacks.forEach { pack ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSelectedIconPack(pack.packageName)
                                    showIconPackPicker = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedIconPack == pack.packageName,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            pack.icon?.let { iconDrawable ->
                                Image(
                                    bitmap = iconDrawable.toBitmap(width = 36, height = 36).asImageBitmap(),
                                    contentDescription = pack.label,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(pack.label, color = palette.textPrimary, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconPackPicker = false }) {
                    Text("Close", color = accentColor)
                }
            },
            containerColor = palette.sheetBackground
        )
    }
}

@Composable
private fun ThemeCardOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    backgroundColor: Color,
    textColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor,
        modifier = modifier
            .height(98.dp)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) accentColor else Color.Gray.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(10.dp)
                        .background(accentColor, CircleShape)
                )
            }
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 4. PRO, ADVANCED & BACKUP PAGES
// -----------------------------------------------------------------------------
@Composable
private fun CascadiaProContent(accentColor: Color, onBack: () -> Unit) {
    val palette = LocalLauncherPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accentColor, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Cascadia Pro", color = palette.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(text = "All Pro features are unlocked", color = palette.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = onBack) { Text("Back", color = palette.textPrimary) }
    }
}

@Composable
private fun AdvancedSettingsContent(
    accentColor: Color,
    onNavigateToBackup: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalLauncherPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SubPageHeader(title = "Advanced", accentColor = accentColor, onBack = onBack)

        ClickableSettingRow(
            icon = Icons.Outlined.Info,
            title = "App info",
            subtitle = "Version 1.16.32",
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )

        ClickableSettingRow(
            icon = Icons.Outlined.Save,
            title = "Backup & Restore (Beta)",
            subtitle = null,
            onClick = onNavigateToBackup
        )

        ClickableSettingRow(
            icon = Icons.Outlined.Home,
            title = "Change default launcher",
            subtitle = null,
            onClick = {
                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                context.startActivity(intent)
            }
        )

        ClickableSettingRow(
            icon = Icons.Outlined.Delete,
            title = "Uninstall launcher",
            subtitle = null,
            onClick = {
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )

        ClickableSettingRow(
            icon = Icons.Outlined.RestartAlt,
            title = "Restart launcher",
            subtitle = "Have you tried turning it off and on again?",
            onClick = {
                val intent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("Privacy policy", color = palette.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
        Text("Terms of Service", color = palette.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
        Text("Open source licenses", color = palette.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun BackupRestoreContent(
    viewModel: LauncherViewModel,
    accentColor: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalLauncherPalette.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val jsonString = viewModel.exportBackupJson(context)
            val success = try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(jsonString.toByteArray())
                }
                true
            } catch (e: Exception) {
                false
            }
            Toast.makeText(context, if (success) "Backup saved!" else "Failed to save backup", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val success = try {
                val jsonString = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                    reader.readText()
                } ?: ""
                viewModel.importBackupJson(context, jsonString)
            } catch (e: Exception) {
                false
            }
            Toast.makeText(context, if (success) "Backup restored!" else "Failed to restore backup", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SubPageHeader(title = "Backup & Restore (Beta)", accentColor = accentColor, onBack = onBack)
        Text("Back up your home screen setup and restore it on any device.", color = palette.textSecondary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))

        ClickableSettingRow(
            icon = Icons.Outlined.FileUpload,
            title = "Create backup",
            subtitle = "Save setup to JSON",
            onClick = { exportLauncher.launch("cascadia_backup.json") }
        )
        ClickableSettingRow(
            icon = Icons.Outlined.RestartAlt,
            title = "Restore backup",
            subtitle = "Load setup from JSON file",
            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
        )
    }
}

// -----------------------------------------------------------------------------
// REUSABLE THEME-AWARE COMPONENTS
// -----------------------------------------------------------------------------
@Composable
private fun SubPageHeader(title: String, accentColor: Color, onBack: () -> Unit) {
    val palette = LocalLauncherPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.18f)
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, accentColor: Color) {
    Text(
        text = title,
        color = accentColor,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCategoryItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    accentColor: Color,
    onClick: () -> Unit
) {
    val palette = LocalLauncherPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 17.sp, color = palette.textPrimary, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(text = subtitle, fontSize = 13.sp, color = palette.textSecondary)
        }
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    accentColor: Color,
    icon: ImageVector? = null,
    onRowClick: (() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalLauncherPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (onRowClick != null) onRowClick() else onCheckedChange(!checked)
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = palette.textPrimary.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = palette.textPrimary, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(text = subtitle, fontSize = 12.sp, color = palette.textSecondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = palette.textSecondary,
                uncheckedTrackColor = palette.cardBackground
            )
        )
    }
}

@Composable
private fun ClickableSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    val palette = LocalLauncherPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = palette.textPrimary.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = palette.textPrimary, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(text = subtitle, fontSize = 12.sp, color = palette.textSecondary)
        }
    }
}
