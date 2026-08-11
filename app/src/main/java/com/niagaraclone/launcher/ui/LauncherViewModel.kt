package com.niagaraclone.launcher.ui

import android.app.Application
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.Process
import android.provider.CalendarContract
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.data.AppShortcutModel
import com.niagaraclone.launcher.data.MediaWidgetTheme
import com.niagaraclone.launcher.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SettingsRepository(application)

    private val _apps = MutableStateFlow<List<AppModel>>(emptyList())
    val apps: StateFlow<List<AppModel>> = _apps.asStateFlow()

    val hiddenPackages = repository.hiddenPackages
    val favoritePackages = repository.favoritePackages
    val selectedMusicPackages = repository.selectedMusicPackages
    val clockStyle = repository.clockStyle
    val accentColorArgb = repository.accentColorArgb
    val homeOffsetY = repository.homeOffsetY
    val widgetHeight = repository.widgetHeight
    val widgetIds = repository.widgetIds
    val batteryStatus = repository.batteryStatus
    val isDefaultLauncher = repository.isDefaultLauncher
    val showMediaWidget = repository.showMediaWidget
    val mediaWidgetTheme = repository.mediaWidgetTheme
    val notificationPermissionGranted = repository.notificationPermissionGranted
    val weatherEnabled = repository.weatherEnabled
    val weatherTempUnit = repository.weatherTempUnit
    val liveWeather = repository.liveWeather
    val quickLockEnabled = repository.quickLockEnabled
    val showBatteryPercentage = repository.showBatteryPercentage
    val cascadiaButtonEnabled = repository.cascadiaButtonEnabled
    val fabTapActionType = repository.fabTapActionType
    val fabTapPackage = repository.fabTapPackage
    val fabSwipeActionType = repository.fabSwipeActionType
    val fabSwipePackage = repository.fabSwipePackage
    val notificationCounts = repository.notificationCounts
    val upcomingEvents = repository.upcomingEvents
    val notificationSummaries = repository.notificationSummaries
    val hapticFeedbackEnabled = repository.hapticFeedbackEnabled
    val rotationEnabled = repository.rotationEnabled
    val selectedIconPack = repository.selectedIconPack
    val weatherCityName = repository.weatherCityName
    val weatherLocationMode = repository.weatherLocationMode

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadInstalledApps()
        }
    }

    init {
        loadInstalledApps()
        refreshPermissions()
        checkDefaultLauncherStatus()
        extractWallpaperAccentColor()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        getApplication<Application>().registerReceiver(packageReceiver, filter)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(packageReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            val pm = getApplication<Application>().packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val appList = resolveInfos.mapNotNull { resolveInfo ->
                val pkgName = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                AppModel(packageName = pkgName, label = label, icon = icon)
            }.sortedBy { it.label.lowercase() }

            _apps.value = appList
        }
    }

    fun extractWallpaperAccentColor() {
        viewModelScope.launch {
            try {
                val wallpaperManager = WallpaperManager.getInstance(getApplication())
                val drawable = wallpaperManager.drawable
                drawable?.let {
                    val bitmap = it.toBitmap(width = 100, height = 100)
                    Palette.from(bitmap).generate { palette ->
                        val vibrantColor = palette?.getVibrantColor(0xFF4DD0E1.toInt())
                            ?: palette?.getDominantColor(0xFF4DD0E1.toInt())
                            ?: 0xFF4DD0E1.toInt()
                        setAccentColorArgb(vibrantColor)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshPermissions() { viewModelScope.launch { repository.checkNotificationPermission() } }
    fun checkDefaultLauncherStatus() { viewModelScope.launch { repository.checkDefaultLauncher() } }
    fun addWidgetId(id: Int) { viewModelScope.launch { repository.addWidgetId(id) } }
    fun trackAppLaunch(packageName: String) {}
    fun launchWebSearch(context: Context, query: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    fun setCustomAppLabel(packageName: String, newLabel: String) {
        viewModelScope.launch {
            _apps.value = _apps.value.map { app ->
                if (app.packageName == packageName) app.copy(label = newLabel) else app
            }
        }
    }

    fun toggleFavorite(packageName: String) { viewModelScope.launch { repository.toggleFavorite(packageName) } }
    fun toggleHidePackage(packageName: String) { viewModelScope.launch { repository.toggleHidePackage(packageName) } }
    fun toggleMusicPackage(packageName: String) { viewModelScope.launch { repository.toggleMusicPackage(packageName) } }
    fun setClockStyle(style: String) { viewModelScope.launch { repository.setClockStyle(style) } }
    fun setAccentColorArgb(color: Int) { viewModelScope.launch { repository.setAccentColorArgb(color) } }
    fun setAccentColor(color: Int) { setAccentColorArgb(color) }
    fun setHomeOffsetY(offsetY: Float) { viewModelScope.launch { repository.setHomeOffsetY(offsetY) } }
    fun removeWidgetId(widgetId: Int) { viewModelScope.launch { repository.removeWidgetId(widgetId) } }
    fun setWeatherEnabled(enabled: Boolean) { viewModelScope.launch { repository.setWeatherEnabled(enabled) } }
    fun setWeatherTempUnit(unit: String) { viewModelScope.launch { repository.setWeatherTempUnit(unit) } }
    fun setShowMediaWidget(show: Boolean) { viewModelScope.launch { repository.setShowMediaWidget(show) } }
    fun setMediaWidgetTheme(theme: MediaWidgetTheme) { viewModelScope.launch { repository.setMediaWidgetTheme(theme) } }
    fun setShowBatteryPercentage(show: Boolean) { viewModelScope.launch { repository.setShowBatteryPercentage(show) } }
    fun setCalendarPreviewEnabled(enabled: Boolean) { viewModelScope.launch { repository.setCalendarPreviewEnabled(enabled) } }
    fun setQuickLockEnabled(enabled: Boolean) { viewModelScope.launch { repository.setQuickLockEnabled(enabled) } }
    fun setCascadiaButtonEnabled(enabled: Boolean) { viewModelScope.launch { repository.setCascadiaButtonEnabled(enabled) } }
    fun setRotationEnabled(enabled: Boolean) { viewModelScope.launch { repository.setRotationEnabled(enabled) } }
    fun setHapticFeedbackEnabled(enabled: Boolean) { viewModelScope.launch { repository.setHapticFeedbackEnabled(enabled) } }
    fun setSelectedIconPack(pack: String) { viewModelScope.launch { repository.setSelectedIconPack(pack) } }
    fun setWeatherCityName(name: String) { viewModelScope.launch { repository.setWeatherCityName(name) } }
    fun setWeatherLocationMode(mode: String) { viewModelScope.launch { repository.setWeatherLocationMode(mode) } }
    fun fetchLiveWeatherForCurrentMode() {}
    fun scanAndFetchLocationWeather() {}

    fun setFabTapAction(actionType: String, pkg: String = "") { viewModelScope.launch { repository.setFabTapAction(actionType, pkg) } }
    fun setFabSwipeAction(actionType: String, pkg: String = "") { viewModelScope.launch { repository.setFabSwipeAction(actionType, pkg) } }

    fun loadCalendarEvents() { viewModelScope.launch { repository.loadCalendarEvents() } }

    fun launchCalendarNewEvent(context: Context) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openCalendarEvent(context: Context, eventId: Long) {
        val uri = Uri.parse("content://com.android.calendar/events/$eventId")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    fun getFolderSubApps(parentPackage: String): List<AppModel> {
        val packageSet = repository.getFolderSubAppPackages(parentPackage)
        return _apps.value.filter { packageSet.contains(it.packageName) }
    }

    fun setFolderSubApps(parentPackage: String, subPackages: Set<String>) {
        viewModelScope.launch { repository.setFolderSubApps(parentPackage, subPackages) }
    }

    fun getAppShortcuts(packageName: String): List<AppShortcutModel> {
        return try {
            val launcherApps = getApplication<Application>().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)
                setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            }
            val shortcuts = launcherApps.getShortcuts(query, Process.myUserHandle()) ?: emptyList()
            shortcuts.map { shortcut ->
                val iconDrawable = launcherApps.getShortcutIconDrawable(shortcut, 0)
                AppShortcutModel(
                    id = shortcut.id,
                    label = shortcut.shortLabel?.toString() ?: shortcut.id,
                    icon = iconDrawable,
                    shortcutInfo = shortcut
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun launchAppShortcut(shortcutInfo: ShortcutInfo) {
        try {
            val launcherApps = getApplication<Application>().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            launcherApps.startShortcut(shortcutInfo, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getRecentlyInstalledApps(): List<AppModel> {
        val pm = getApplication<Application>().packageManager
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

        return _apps.value.filter { app ->
            try {
                val info = pm.getPackageInfo(app.packageName, 0)
                info.firstInstallTime >= thirtyDaysAgo
            } catch (e: Exception) {
                false
            }
        }.sortedByDescending { app ->
            try { pm.getPackageInfo(app.packageName, 0).firstInstallTime } catch (e: Exception) { 0L }
        }
    }

    fun exportBackupJson(): String = repository.exportBackupJson()
    fun importBackupJson(jsonStr: String): Boolean = repository.importBackupJson(jsonStr)
}
