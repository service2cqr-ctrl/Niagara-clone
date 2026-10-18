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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.niagaraclone.launcher.ui.theme.AccentPresets
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
        if (currentPage == SettingsPage.MAIN) {
            onDismiss()
        } else {
            currentPage = SettingsPage.MAIN
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
                viewModel = viewModel,
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
            subtitle = null,
            accentColor = accentColor,
            onClick = { onNavigate(SettingsPage.PRO) }
        )

        SettingsCategoryItem(
            icon = Icons.Outlined.HelpOutline,
            title = "Help & User Guide",
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

    var showCalendarDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var isWeatherSheetOpen by remember { mutableStateOf(false) }
    var showMusicAppSheet by remember { mutableStateOf(false) }
    var showFolderInfoDialog by remember { mutableStateOf(false) }
    var showSmartSearchDialog by remember { mutableStateOf(false) }

    val rawMediaWidgetEnabled by viewModel.showMediaWidget.collectAsState()
    val selectedMediaTheme by viewModel.mediaWidgetTheme.collectAsState()
    val isPermissionGranted by viewModel.notificationPermissionGranted.collectAsState()
    val isMediaWidgetActive = rawMediaWidgetEnabled && isPermissionGranted

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

    var showMediaThemeDialog by remember { mutableStateOf(false) }

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
        ToggleSettingRow("Quick Replies", "Reply right from your home screen", quickRepliesEnabled, accentColor) {
            coroutineScope.launch { viewModel.repository.setQuickRepliesEnabled(it) }
        }

        ClickableSettingRow(
            icon = Icons.Outlined.CalendarMonth,
            title = "Calendar Agenda",
            subtitle = "Open your calendar agenda by tapping the date",
            onClick = { showCalendarDialog = true }
        )
        ToggleSettingRow(
            title = "Calendar Preview",
            subtitle = "See upcoming events on your home screen",
            checked = calendarPreviewEnabled,
            accentColor = accentColor,
            onCheckedChange = { enabled ->
                coroutineScope.launch { viewModel.repository.setCalendarPreviewEnabled(enabled) }
            }
        )

        ClickableSettingRow(
            icon = Icons.Outlined.WbSunny,
            title = "Weather Forecast",
            subtitle = "See the full forecast by tapping the current weather",
            onClick = { isWeatherSheetOpen = true }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMediaThemeDialog = true }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Media Player", fontSize = 16.sp, color = Color.White)
                Text(
                    text = "Theme: ${when (selectedMediaTheme) {
                        MediaWidgetTheme.FROSTED_GLASS -> "Frosted Glass"
                        MediaWidgetTheme.DARK -> "Dark"
                        MediaWidgetTheme.LIGHT -> "Light"
                        MediaWidgetTheme.MATCH_ACCENT -> "Match Accent"
                        MediaWidgetTheme.ALBUM_ART -> "Album Art"
                    }}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Switch(
                checked = isMediaWidgetActive,
                onCheckedChange = { checked ->
                    viewModel.setShowMediaWidget(checked)
                    if (checked && !isPermissionGranted) {
                        showPermissionDialog = true
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = accentColor
                )
            )
        }

        ClickableSettingRow(
            icon = Icons.Outlined.MusicNote,
            title = "Music Apps",
            subtitle = "Show music apps when connecting headphones or speakers",
            onClick = { showMusicAppSheet = true }
        )

        SectionHeader("Digital Wellbeing", accentColor)
        ToggleSettingRow("Usage Breaker", "Get reminders to take breaks", usageBreakerEnabled, accentColor) {
            coroutineScope.launch { viewModel.repository.setUsageBreakerEnabled(it) }
        }
        ToggleSettingRow("Notification Summary", "Bundle notifications to reduce distractions", notificationSummaryEnabled, accentColor) {
            coroutineScope.launch { viewModel.repository.setNotificationSummaryEnabled(it) }
        }

        SectionHeader("App Organization", accentColor)
        ClickableSettingRow(
            icon = Icons.Outlined.VisibilityOff,
            title = "Hide Apps",
            subtitle = "Hide apps from your app list",
            onClick = onNavigateToHideApps
        )
        ClickableSettingRow(
            icon = Icons.Outlined.Folder,
            title = "Folders & Pop-ups",
            subtitle = "Group apps to keep your app list tidy",
            onClick = { showFolderInfoDialog = true }
        )
        ToggleSettingRow("Suggest Often-Used Apps", "Directly access often-used apps from your favorites", suggestAppsEnabled, accentColor) {
            coroutineScope.launch { viewModel.repository.setSuggestAppsEnabled(it) }
        }
        ToggleSettingRow("Sort All Apps List By Usage", "Prioritize most-used apps for each letter", sortByUsageEnabled, accentColor) {
            coroutineScope.launch { viewModel.repository.setSortByUsageEnabled(it) }
        }

        SectionHeader("Smart Search", accentColor)
        ClickableSettingRow(
            icon = Icons.Outlined.Search,
            title = "Smart Search",
            subtitle = "Apps, contacts, and web in one place",
            onClick = { showSmartSearchDialog = true }
        )

        SectionHeader("Shortcuts & Gestures", accentColor)
        ToggleSettingRow(
            title = "Cascadia Button",
            subtitle = "Run quick actions from your home screen",
            checked = cascadiaButtonEnabled,
            accentColor = accentColor,
            onCheckedChange = { viewModel.setCascadiaButtonEnabled(it) }
        )
        ToggleSettingRow(
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
            title = "Show Battery Percentage",
            subtitle = "Display battery level next to date",
            checked = showBatteryPercentage,
            accentColor = accentColor,
            onCheckedChange = { viewModel.setShowBatteryPercentage(it) }
        )
        ToggleSettingRow(
            title = "Allow Home Screen Rotation",
            subtitle = "When device is rotated",
            checked = rotationEnabled,
            accentColor = accentColor,
            onCheckedChange = { viewModel.setRotationEnabled(it) }
        )
        ToggleSettingRow(
            title = "Allow Haptic Feedback",
            subtitle = "Follow system settings",
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
            title = { Text("Folders & Pop-ups", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Swipe right on any favorite app on your home screen to open its Pop-up folder.", color = Color.Gray) },
            confirmButton = { TextButton(onClick = { showFolderInfoDialog = false }) { Text("Got it", color = accentColor) } },
            containerColor = Color(0xFF161616)
        )
    }
    if (showSmartSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSmartSearchDialog = false },
            title = { Text("Smart Search Settings", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                ToggleSettingRow("Include Contacts", "Search phone contacts and dial directly", searchContactsEnabled, accentColor) {
                    coroutineScope.launch { viewModel.repository.setSearchContactsEnabled(it) }
                }
            },
            confirmButton = { TextButton(onClick = { showSmartSearchDialog = false }) { Text("Done", color = accentColor) } },
            containerColor = Color(0xFF161616)
        )
    }
}

// -----------------------------------------------------------------------------
// 3. THEMES PAGE (Featuring Pitch Black, Light, Glass & Accent Color Picker)
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

    var showColorPicker by remember { mutableStateOf(false) }
    var showIconPackPicker by remember { mutableStateOf(false) }

    val iconPackManager = remember { IconPackManager(context) }
    val installedIconPacks = remember { iconPackManager.getInstalledIconPacks() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SubPageHeader(title = "Themes", accentColor = accentColor, onBack = onBack)

        SectionHeader("Theme Style", accentColor)

        // 3 Theme Options: Pitch Black, Light, Glass
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ThemeCardOption(
                title = "Pitch Black",
                isSelected = currentAppTheme == AppTheme.PITCH_BLACK,
                backgroundColor = Color.Black,
                textColor = Color.White,
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                onClick = { coroutineScope.launch { viewModel.setAppTheme(AppTheme.PITCH_BLACK) } }
            )
            ThemeCardOption(
                title = "Light",
                isSelected = currentAppTheme == AppTheme.LIGHT,
                backgroundColor = Color(0xFFFAFAFA),
                textColor = Color(0xFF121212),
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                onClick = { coroutineScope.launch { viewModel.setAppTheme(AppTheme.LIGHT) } }
            )
            ThemeCardOption(
                title = "Glass",
                isSelected = currentAppTheme == AppTheme.GLASS,
                backgroundColor = accentColor.copy(alpha = 0.25f),
                textColor = Color.White,
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                onClick = { coroutineScope.launch { viewModel.setAppTheme(AppTheme.GLASS) } }
            )
        }

        SectionHeader("Customization", accentColor)
        ClickableSettingRow(
            icon = Icons.Outlined.Palette,
            title = "Accent Color",
            subtitle = "Customize home screen clock and accent colors",
            onClick = { showColorPicker = true }
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
            title = { Text("Choose Accent Color", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentPresets.forEach { colorPreset ->
                        val colorVal = colorPreset.hashCode() // or ColorInt
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAccentColor(colorPreset.hashCode()) // stores int
                                    showColorPicker = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(28.dp).background(colorPreset, CircleShape))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "Preset color", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            if (accentColorArgb == colorPreset.hashCode()) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = colorPreset)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showColorPicker = false }) { Text("Close", color = accentColor) } },
            containerColor = Color(0xFF161616)
        )
    }

    if (showIconPackPicker) {
        AlertDialog(
            onDismissRequest = { showIconPackPicker = false },
            title = { Text("Select Icon Pack", color = Color.White, fontWeight = FontWeight.Bold) },
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
                        RadioButton(selected = selectedIconPack == "SYSTEM" || selectedIconPack.isEmpty(), onClick = null, colors = RadioButtonDefaults.colors(selectedColor = accentColor))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Default System Icons", color = Color.White, fontSize = 16.sp)
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
                            RadioButton(selected = selectedIconPack == pack.packageName, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = accentColor))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(pack.label, color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showIconPackPicker = false }) { Text("Close", color = accentColor) } },
            containerColor = Color(0xFF161616)
        )
    }
}

@Composable
private fun ThemeCardOption(
    title: String,
    isSelected: Boolean,
    backgroundColor: Color,
    textColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = modifier
            .height(90.dp)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) accentColor else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = title,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(10.dp)
                        .background(accentColor, CircleShape)
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
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accentColor, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Cascadia Pro", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onBack) { Text("Back", color = Color.White) }
    }
}

@Composable
private fun AdvancedSettingsContent(
    viewModel: LauncherViewModel,
    accentColor: Color,
    onNavigateToBackup: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        SubPageHeader(title = "Advanced", accentColor = accentColor, onBack = onBack)
        ClickableSettingRow(icon = Icons.Outlined.Save, title = "Backup & Restore (Beta)", subtitle = null, onClick = onNavigateToBackup)
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
    }
}

@Composable
private fun BackupRestoreContent(viewModel: LauncherViewModel, accentColor: Color, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp)) {
        SubPageHeader(title = "Backup & Restore (Beta)", accentColor = accentColor, onBack = onBack)
        Text("Back up your home screen setup and restore it on any device.", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))
        ClickableSettingRow(icon = Icons.Outlined.FileUpload, title = "Create backup", subtitle = "Save setup to JSON", onClick = {
            val json = viewModel.exportBackupJson()
            Toast.makeText(context, "Backup snapshot generated", Toast.LENGTH_SHORT).show()
        })
    }
}

@Composable
private fun SubPageHeader(title: String, accentColor: Color, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(shape = CircleShape, color = accentColor.copy(alpha = 0.2f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentColor, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, accentColor: Color) {
    Text(text = title, color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
}

@Composable
private fun SettingsCategoryItem(icon: ImageVector, title: String, subtitle: String?, accentColor: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 17.sp, color = Color.White, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun ToggleSettingRow(title: String, subtitle: String?, checked: Boolean, accentColor: Color, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, color = Color.White)
            if (subtitle != null) Text(text = subtitle, fontSize = 11.sp, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = accentColor))
    }
}

@Composable
private fun ClickableSettingRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, color = Color.White)
            if (subtitle != null) Text(text = subtitle, fontSize = 11.sp, color = Color.Gray)
        }
    }
}
