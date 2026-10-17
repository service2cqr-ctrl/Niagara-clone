package com.niagaraclone.launcher.ui

import android.Manifest
import android.app.Application
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Process
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.niagaraclone.launcher.backup.BackupManager
import com.niagaraclone.launcher.data.*
import com.niagaraclone.launcher.services.MediaNotificationListener
import com.niagaraclone.launcher.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SettingsRepository(application)
    private val appRepository = AppRepository(application)
    private val weatherRepository = WeatherRepository(application)
    private val backupManager = BackupManager(application, repository)

    val apps: StateFlow<List<AppModel>> = appRepository.installedApps

    val theme = repository.appTheme
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
    val hapticFeedbackEnabled = repository.hapticFeedbackEnabled
    val rotationEnabled = repository.rotationEnabled
    val selectedIconPack = repository.selectedIconPack
    val weatherCityName = repository.weatherCityName
    val weatherLocationMode = repository.weatherLocationMode
    val searchContactsEnabled = repository.searchContactsEnabled

    val quickRepliesEnabled = repository.quickRepliesEnabled
    val usageBreakerEnabled = repository.usageBreakerEnabled
    val notificationSummaryEnabled = repository.notificationSummaryEnabled
    val suggestAppsEnabled = repository.suggestAppsEnabled
    val sortByUsageEnabled = repository.sortByUsageEnabled

    // Live notification counts and summaries from MediaNotificationListener service
    val notificationCounts = MediaNotificationListener.activeNotificationCounts
    val notificationSummaries = MediaNotificationListener.notificationSummaries
    val upcomingEvents = repository.upcomingEvents

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
        fetchLiveWeatherForCurrentMode()

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
            appRepository.loadInstalledApps(repository.selectedIconPack.value)
        }
    }

    fun refreshApps() { loadInstalledApps() }

    fun fetchLiveWeatherForCurrentMode() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                if (hasFine || hasCoarse) {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    val location = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        ?: locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                    if (location != null) {
                        val weatherData = weatherRepository.fetchLiveWeather(location.latitude, location.longitude)
                        if (weatherData != null) {
                            repository.updateLiveWeather(weatherData.temperatureCelsius, weatherData.conditionIcon)
                            repository.setWeatherCityName(weatherData.cityName)
                        }
                    } else {
                        // Fallback default coordinates (Pune/India) if GPS is null
                        val weatherData = weatherRepository.fetchLiveWeather(18.5204, 73.8567)
                        if (weatherData != null) {
                            repository.updateLiveWeather(weatherData.temperatureCelsius, weatherData.conditionIcon)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
    fun trackAppLaunch(packageName: String = "") {}

    fun launchWebSearch(context: Context, query: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    fun setCustomAppLabel(packageName: String, newLabel: String) {
        if (packageName.isBlank()) return
        viewModelScope.launch {
            appRepository.setCustomAppLabel(packageName, newLabel)
        }
    }

    fun setAppTheme(theme: AppTheme) { viewModelScope.launch { repository.setAppTheme(theme) } }

    fun toggleFavorite(packageName: String = "") { 
        if (packageName.isNotEmpty()) {
            viewModelScope.launch { repository.toggleFavorite(packageName) } 
        }
    }

    fun toggleHidePackage(packageName: String = "") { 
        if (packageName.isNotEmpty()) {
            viewModelScope.launch { repository.toggleHidePackage(packageName) } 
        }
    }

    fun toggleMusicPackage(packageName: String = "") { 
        if (packageName.isNotEmpty()) {
            viewModelScope.launch { repository.toggleMusicPackage(packageName) } 
        }
    }

    fun setClockStyle(style: String) { viewModelScope.launch { repository.setClockStyle(style) } }
    fun setAccentColorArgb(color: Int) { viewModelScope.launch { repository.setAccentColorArgb(color) } }
    fun setAccentColor(color: Int) { setAccentColorArgb(color) }
    fun setHomeOffsetY(offsetY: Float) { viewModelScope.launch { repository.setHomeOffsetY(offsetY) } }
    fun setWidgetHeight(height: Int) { viewModelScope.launch { repository.setWidgetHeight(height) } }
    fun removeWidgetId(widgetId: Int) { viewModelScope.launch { repository.removeWidgetId(widgetId) } }
    fun setWeatherEnabled(enabled: Boolean) { viewModelScope.launch { repository.setWeatherEnabled(enabled) } }
    fun setWeatherTempUnit(unit: String) { viewModelScope.launch { repository.setWeatherTempUnit(unit) } }
    fun setShowMediaWidget(show: Boolean) { viewModelScope.launch { repository.setShowMediaWidget(show) } }
    fun setMediaWidgetTheme(theme: MediaWidgetTheme) { viewModelScope.launch { repository.setMediaWidgetTheme(theme) } }
    fun setShowBatteryPercentage(show: Boolean) { viewModelScope.launch { repository.setShowBatteryPercentage(show) } }
    fun setCalendarPreviewEnabled(enabled: Boolean) { viewModelScope.launch { repository.setCalendarPreviewEnabled(enabled) } }
    fun setSearchContactsEnabled(enabled: Boolean) { viewModelScope.launch { repository.setSearchContactsEnabled(enabled) } }
    fun setQuickLockEnabled(enabled: Boolean) { viewModelScope.launch { repository.setQuickLockEnabled(enabled) } }
    fun setCascadiaButtonEnabled(enabled: Boolean) { viewModelScope.launch { repository.setCascadiaButtonEnabled(enabled) } }
    fun setRotationEnabled(enabled: Boolean) { viewModelScope.launch { repository.setRotationEnabled(enabled) } }
    fun setHapticFeedbackEnabled(enabled: Boolean) { viewModelScope.launch { repository.setHapticFeedbackEnabled(enabled) } }
    
    fun setSelectedIconPack(pack: String) {
        viewModelScope.launch {
            repository.setSelectedIconPack(pack)
            appRepository.loadInstalledApps(pack)
        }
    }

    fun setWeatherCityName(name: String) { viewModelScope.launch { repository.setWeatherCityName(name) } }
    fun setWeatherLocationMode(mode: String) { viewModelScope.launch { repository.setWeatherLocationMode(mode) } }

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

    fun getFolderSubApps(parentPackage: String = ""): List<AppModel> {
        if (parentPackage.isEmpty()) return emptyList()
        val packageSet = repository.getFolderSubAppPackages(parentPackage)
        return apps.value.filter { packageSet.contains(it.packageName) }
    }

    fun setFolderSubApps(parentPackage: String = "", subPackages: Set<String> = emptySet()) {
        if (parentPackage.isEmpty()) return
        viewModelScope.launch { repository.setFolderSubApps(parentPackage, subPackages) }
    }

    fun getAppShortcuts(packageName: String = ""): List<AppShortcutModel> {
        if (packageName.isBlank()) return emptyList()
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
                    packageName = shortcut.`package`,
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

        return apps.value.filter { app ->
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

    fun exportBackupJson(context: Context = getApplication()): String = backupManager.snapshot()
    fun importBackupJson(context: Context = getApplication(), jsonStr: String): Boolean = backupManager.restore(jsonStr)

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val application = context.applicationContext as Application
            return LauncherViewModel(application) as T
        }
    }
}
