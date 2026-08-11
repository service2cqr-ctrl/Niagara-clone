package com.niagaraclone.launcher.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.components.MediaWidgetTheme
import com.niagaraclone.launcher.ui.components.MusicAppSelectionSheet
import com.niagaraclone.launcher.ui.components.UserGuideSheet
import com.niagaraclone.launcher.ui.components.WeatherBottomSheet
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

    BackHandler {
        if (currentPage == SettingsPage.MAIN) {
            onDismiss()
        } else {
            currentPage = SettingsPage.MAIN
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        when (currentPage) {
            SettingsPage.MAIN -> MainSettingsContent(
                onNavigate = { currentPage = it },
                onOpenUserGuide = { showUserGuideSheet = true },
                onDismiss = onDismiss
            )
            SettingsPage.PRODUCTIVITY -> ProductivitySettingsContent(
                viewModel = viewModel,
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
                onBack = { currentPage = SettingsPage.MAIN }
            )
            SettingsPage.PRO -> CascadiaProContent(
                onBack = { currentPage = SettingsPage.MAIN }
            )
            SettingsPage.ADVANCED -> AdvancedSettingsContent(
                onNavigateToBackup = { currentPage = SettingsPage.BACKUP_RESTORE },
                onBack = { currentPage = SettingsPage.MAIN }
            )
            SettingsPage.BACKUP_RESTORE -> BackupRestoreContent(
                viewModel = viewModel,
                onBack = { currentPage = SettingsPage.ADVANCED }
            )
            SettingsPage.HIDE_APPS -> HideAppsScreen(
                viewModel = viewModel,
                onBack = { currentPage = SettingsPage.PRODUCTIVITY }
            )
        }
    }

    if (showUserGuideSheet) {
        UserGuideSheet(onDismiss = { showUserGuideSheet = false })
    }
}

// -----------------------------------------------------------------------------
// 1. MAIN SETTINGS HOME
// -----------------------------------------------------------------------------
@Composable
private fun MainSettingsContent(
    onNavigate: (SettingsPage) -> Unit,
    onOpenUserGuide: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to Home", tint = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF4DD0E1),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cascadia settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        SettingsCategoryItem(
            icon = Icons.Outlined.StarBorder,
            title = "Productivity",
            subtitle = "Stay efficient and organized",
            onClick = { onNavigate(SettingsPage.PRODUCTIVITY) }
        )

        SettingsCategoryItem(
            icon = Icons.Outlined.Palette,
            title = "Themes",
            subtitle = "Give your home screen your personal touch",
            onClick = { onNavigate(SettingsPage.THEMES) }
        )

        SettingsCategoryItem(
            icon = Icons.Outlined.AutoAwesome,
            title = "Cascadia Pro",
            subtitle = null,
            onClick = { onNavigate(SettingsPage.PRO) }
        )

        SettingsCategoryItem(
            icon = Icons.Outlined.HelpOutline,
            title = "Help & User Guide",
            subtitle = "Learn gestures and launcher shortcuts",
            onClick = onOpenUserGuide
        )

        SettingsCategoryItem(
            icon = Icons.Outlined.Public,
            title = "Community",
            subtitle = null,
            onClick = { }
        )

        SettingsCategoryItem(
            icon = Icons.Outlined.MoreHoriz,
            title = "Advanced",
            subtitle = null,
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

    val calendarPreviewEnabled by viewModel.repository.calendarPreviewEnabled.collectAsState(initial = true)
    val searchContactsEnabled by viewModel.repository.searchContactsEnabled.collectAsState(initial = true)

    var showMediaThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SubPageHeader(title = "Productivity", onBack = onBack)

        SectionHeader("Instant Access")
        ToggleSettingRow("Quick Replies", "Reply right from your home screen", true) {}

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
                    checkedTrackColor = Color(0xFF4DD0E1)
                )
            )
        }

        ClickableSettingRow(
            icon = Icons.Outlined.MusicNote,
            title = "Music Apps",
            subtitle = "Show music apps when connecting headphones or speakers",
            onClick = { showMusicAppSheet = true }
        )

        SectionHeader("Digital Wellbeing")
        ToggleSettingRow("Usage Breaker", "Get reminders to take breaks", false) {}
        ToggleSettingRow("Notification Summary", "Bundle notifications to reduce distractions", false) {}

        SectionHeader("App Organization")
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
        ToggleSettingRow("Suggest Often-Used Apps", "Directly access often-used apps from your favorites", false) {}
        ToggleSettingRow("Sort All Apps List By Usage", "Prioritize most-used apps for each letter", false) {}

        SectionHeader("Smart Search")
        ClickableSettingRow(
            icon = Icons.Outlined.Search,
            title = "Smart Search",
            subtitle = "Apps, contacts, and web in one place",
            onClick = { showSmartSearchDialog = true }
        )

        SectionHeader("Shortcuts & Gestures")
        ToggleSettingRow(
            title = "Cascadia Button",
            subtitle = "Run quick actions from your home screen",
            checked = cascadiaButtonEnabled,
            onCheckedChange = { viewModel.setCascadiaButtonEnabled(it) }
        )
        ToggleSettingRow(
            title = "Quick Lock",
            subtitle = "Double-tap empty home screen space to lock phone",
            checked = quickLockEnabled,
            onCheckedChange = { viewModel.setQuickLockEnabled(it) }
        )

        SectionHeader("Advanced")
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
            onCheckedChange = { viewModel.setShowBatteryPercentage(it) }
        )
        ToggleSettingRow(
            title = "Allow Home Screen Rotation",
            subtitle = "When device is rotated",
            checked = rotationEnabled,
            onCheckedChange = { viewModel.setRotationEnabled(it) }
        )
        ToggleSettingRow(
            title = "Allow Haptic Feedback",
            subtitle = "Follow system settings",
            checked = hapticFeedbackEnabled,
            onCheckedChange = { viewModel.setHapticFeedbackEnabled(it) }
        )
    }

    if (isWeatherSheetOpen) {
        WeatherBottomSheet(
            viewModel = viewModel,
            onDismiss = { isWeatherSheetOpen = false }
        )
    }

    if (showMusicAppSheet) {
        MusicAppSelectionSheet(
            viewModel = viewModel,
            onDismiss = { showMusicAppSheet = false }
        )
    }

    if (showFolderInfoDialog) {
        AlertDialog(
            onDismissRequest = { showFolderInfoDialog = false },
            title = { Text("Folders & Pop-ups", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Swipe right on any favorite app on your home screen to open its Pop-up folder.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "You can add sub-apps and launch system shortcuts directly from the pop-up view.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderInfoDialog = false }) {
                    Text("Got it", color = Color(0xFF4DD0E1))
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    if (showSmartSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSmartSearchDialog = false },
            title = { Text("Smart Search Settings", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ToggleSettingRow(
                        title = "Include Contacts",
                        subtitle = "Search phone contacts and dial directly",
                        checked = searchContactsEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch { viewModel.repository.setSearchContactsEnabled(enabled) }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSmartSearchDialog = false }) {
                    Text("Done", color = Color(0xFF4DD0E1))
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    if (showMediaThemeDialog) {
        AlertDialog(
            onDismissRequest = { showMediaThemeDialog = false },
            title = { Text("Media Player Theme", color = Color.White) },
            text = {
                Column {
                    MediaWidgetTheme.values().forEach { theme ->
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
                                    selectedColor = Color(0xFF4DD0E1),
                                    unselectedColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (theme) {
                                    MediaWidgetTheme.FROSTED_GLASS -> "Frosted Glass"
                                    MediaWidgetTheme.DARK -> "Dark"
                                    MediaWidgetTheme.LIGHT -> "Light"
                                },
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMediaThemeDialog = false }) {
                    Text("Close", color = Color(0xFF4DD0E1))
                }
            },
            containerColor = Color.Black
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(text = "Notification Access Needed", color = Color.White) },
            text = {
                Text(
                    text = "To display live track info and album art, Cascadia Launcher requires Notification Access in system settings.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        openNotificationAccessSettings(context)
                    }
                ) {
                    Text(text = "Open Settings", color = Color(0xFF4DD0E1))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(text = "Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.Black
        )
    }

    if (showCalendarDialog) {
        AlertDialog(
            onDismissRequest = { showCalendarDialog = false },
            title = { Text("Calendar Agenda", color = Color.White) },
            text = {
                Column {
                    Text("Check your plans and upcoming events by tapping the date", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    ToggleSettingRow(
                        title = "Show Calendar Preview",
                        subtitle = "Display next event under date",
                        checked = calendarPreviewEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch { viewModel.repository.setCalendarPreviewEnabled(enabled) }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalendarDialog = false }) {
                    Text("Done", color = Color(0xFF4DD0E1))
                }
            },
            containerColor = Color.Black
        )
    }
}

// -----------------------------------------------------------------------------
// 3. THEMES PAGE
// -----------------------------------------------------------------------------
@Composable
private fun ThemesSettingsContent(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    var showColorPicker by remember { mutableStateOf(false) }

    val colorPresets = listOf(
        0xFF4DD0E1.toInt() to "Teal (Default)",
        0xFF3B82F6.toInt() to "Blue",
        0xFF8B5CF6.toInt() to "Purple",
        0xFF10B981.toInt() to "Emerald",
        0xFFF59E0B.toInt() to "Amber",
        0xFFF43F5E.toInt() to "Rose"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SubPageHeader(title = "Themes", onBack = onBack)

        ClickableSettingRow(
            icon = Icons.Outlined.Palette,
            title = "Accent Color",
            subtitle = "Customize home screen clock and accent colors",
            onClick = { showColorPicker = true }
        )
        ClickableSettingRow(
            icon = Icons.Outlined.AddCircleOutline,
            title = "Create Theme",
            subtitle = "Build a new theme from scratch",
            onClick = { }
        )
        ClickableSettingRow(
            icon = Icons.Outlined.Edit,
            title = "Edit Current Theme",
            subtitle = "Fine-tune your setup",
            onClick = { }
        )
        ClickableSettingRow(
            icon = Icons.Outlined.Folder,
            title = "My Themes",
            subtitle = "Your custom creations and imported themes",
            onClick = { }
        )
        ClickableSettingRow(
            icon = Icons.Outlined.Brush,
            title = "Ready-Made Themes",
            subtitle = "Pick from handcrafted themes with just one tap",
            onClick = { }
        )
        ClickableSettingRow(
            icon = Icons.Outlined.FavoriteBorder,
            title = "Community Themes",
            subtitle = "Discover themes made by our community",
            onClick = { }
        )
    }

    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Choose Accent Color", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    colorPresets.forEach { (colorVal, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAccentColor(colorVal)
                                    showColorPicker = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(colorVal), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = name, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            if (accentColorArgb == colorVal) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(colorVal))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) { Text("Close", color = Color(accentColorArgb)) }
            },
            containerColor = Color.Black
        )
    }
}

// -----------------------------------------------------------------------------
// 4. PRO MEMBERSHIP PAGE
// -----------------------------------------------------------------------------
@Composable
private fun CascadiaProContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFF4DD0E1),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Member since", color = Color.Gray, fontSize = 14.sp)
        Text(text = "21 Nov 2023", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(text = "Thank you for your support.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(48.dp))

        TextButton(onClick = { }) { Text("Discover features", color = Color.White, fontSize = 16.sp) }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { }) { Text("Manage membership", color = Color.Gray, fontSize = 16.sp) }
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onBack) { Text("Back", color = Color.White) }
    }
}

// -----------------------------------------------------------------------------
// 5. ADVANCED PAGE
// -----------------------------------------------------------------------------
@Composable
private fun AdvancedSettingsContent(
    onNavigateToBackup: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SubPageHeader(title = "Advanced", onBack = onBack)

        ClickableSettingRow(
            icon = Icons.Outlined.Info,
            title = "App info",
            subtitle = "Version 1.16.18",
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
            icon = Icons.Outlined.Description,
            title = "Changelog",
            subtitle = null,
            onClick = { }
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
            onClick = { }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Privacy policy", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp).clickable { })
        Text(text = "Terms of Service", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp).clickable { })
        Text(text = "Privacy Settings", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp).clickable { })
        Text(text = "Open source licences", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp).clickable { })
    }
}

// -----------------------------------------------------------------------------
// 6. BACKUP & RESTORE PAGE
// -----------------------------------------------------------------------------
@Composable
private fun BackupRestoreContent(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val success = viewModel.exportBackupJson(context, it)
            Toast.makeText(context, if (success) "Backup saved successfully!" else "Failed to save backup", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val success = viewModel.importBackupJson(context, it)
            Toast.makeText(context, if (success) "Backup restored successfully!" else "Failed to restore backup", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SubPageHeader(title = "Backup & Restore (Beta)", onBack = onBack)

        Text(text = "Back up your home screen setup and restore it on any device.", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))

        FeatureBenefitRow(
            icon = Icons.Outlined.Sync,
            title = "Effortlessly switch devices",
            description = "Apply your home screen setup to your new device in seconds."
        )

        FeatureBenefitRow(
            icon = Icons.Outlined.Security,
            title = "Keep your setup",
            description = "Favorites, icons, pop-up folders, settings and more are kept safe."
        )

        FeatureBenefitRow(
            icon = Icons.Outlined.Close,
            title = "Not included",
            description = "We cannot back up externally set wallpapers and shortcuts.",
            isNegative = true
        )

        Spacer(modifier = Modifier.height(32.dp))
        Text("Actions", color = Color(0xFF4DD0E1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

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
// REUSABLE UI COMPONENTS
// -----------------------------------------------------------------------------
@Composable
private fun SubPageHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF4DD0E1),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
    )
}

@Composable
private fun SettingsCategoryItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String?,
    initialValue: Boolean = false,
    checked: Boolean = initialValue,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = Color.White)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFF4DD0E1)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = Color.White)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun FeatureBenefitRow(
    icon: ImageVector,
    title: String,
    description: String,
    isNegative: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (isNegative) Color(0xFF331111) else Color(0xFF113333),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isNegative) Color(0xFFEF4444) else Color(0xFF4DD0E1),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = description, color = Color.Gray, fontSize = 13.sp)
        }
    }
}
