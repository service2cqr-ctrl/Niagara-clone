package com.niagaraclone.launcher.ui

import android.annotation.SuppressLint
import android.app.Application
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.data.AppTheme
import com.niagaraclone.launcher.data.CalendarEventModel
import com.niagaraclone.launcher.data.CalendarRepository
import com.niagaraclone.launcher.data.SettingsRepository
import com.niagaraclone.launcher.data.WeatherData
import com.niagaraclone.launcher.data.WeatherRepository
import com.niagaraclone.launcher.services.MediaNotificationListener
import com.niagaraclone.launcher.services.NotificationSummaryModel
import com.niagaraclone.launcher.ui.components.MediaWidgetTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LauncherViewModel(
    val repository: SettingsRepository,
    private val context: Context
) : ViewModel() {

    private val weatherRepository = WeatherRepository(context)
    private val calendarRepository = CalendarRepository(context)
    private val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

    private val _apps = MutableStateFlow<List<AppModel>>(emptyList())
    val apps: StateFlow<List<AppModel>> = _apps.asStateFlow()

    private val _batteryStatus = MutableStateFlow("100%")
    val batteryStatus: StateFlow<String> = _batteryStatus.asStateFlow()

    private val _isDefaultLauncher = MutableStateFlow(true)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    private val _isBannerDismissedSession = MutableStateFlow(false)
    val isBannerDismissedSession: StateFlow<Boolean> = _isBannerDismissedSession.asStateFlow()

    private val _notificationPermissionGranted = MutableStateFlow(false)
    val notificationPermissionGranted: StateFlow<Boolean> = _notificationPermissionGranted.asStateFlow()

    // --- CALENDAR & NOTIFICATION STATES ---
    private val _upcomingEvents = MutableStateFlow<List<CalendarEventModel>>(emptyList())
    val upcomingEvents: StateFlow<List<CalendarEventModel>> = _upcomingEvents.asStateFlow()

    val notificationCounts: StateFlow<Map<String, Int>> = MediaNotificationListener.activeNotificationCounts
    val notificationSummaries: StateFlow<List<NotificationSummaryModel>> = MediaNotificationListener.notificationSummaries

    // --- USAGE BREAKER LIMIT ---
    private val _usageBreakerLimit = MutableStateFlow(15)
    val usageBreakerLimit: StateFlow<Int> = _usageBreakerLimit.asStateFlow()

    // --- HIDDEN PACKAGES STATE ---
    private val _hiddenPackages = MutableStateFlow<Set<String>>(emptySet())
    val hiddenPackages: StateFlow<Set<String>> = _hiddenPackages.asStateFlow()

    // --- MUSIC APPS SELECTION STATE ---
    private val _selectedMusicPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedMusicPackages: StateFlow<Set<String>> = _selectedMusicPackages.asStateFlow()

    // --- LIVE WEATHER STATES ---
    private val _liveWeather = MutableStateFlow<WeatherData?>(null)
    val liveWeather: StateFlow<WeatherData?> = _liveWeather.asStateFlow()

    val theme: StateFlow<AppTheme> = repository.theme
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = AppTheme.SYSTEM)

    val accentColorArgb: StateFlow<Int> = repository.accentColor
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0xFF4DD0E1.toInt())

    val favoritePackages: StateFlow<Set<String>> = repository.favorites
        .map { it.toSet() }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptySet())

    val homeOffsetY: StateFlow<Float> = repository.homeOffsetY
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0f)

    val widgetHeight: StateFlow<Float> = repository.widgetHeight
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 180f)

    val widgetIds: StateFlow<List<Int>> = repository.widgetIds
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val showMediaWidget: StateFlow<Boolean> = repository.showMediaWidget
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)

    val mediaWidgetTheme: StateFlow<MediaWidgetTheme> = repository.mediaWidgetThemeName
        .map { name ->
            try { MediaWidgetTheme.valueOf(name) } catch (e: Exception) { MediaWidgetTheme.FROSTED_GLASS }
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = MediaWidgetTheme.FROSTED_GLASS)

    // Weather Preference Flows
    val weatherEnabled: StateFlow<Boolean> = repository.weatherEnabled
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)

    val weatherLocationMode: StateFlow<String> = repository.weatherLocationMode
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "AUTO")

    val weatherCityName: StateFlow<String> = repository.weatherCityName
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "Vadodara")

    val weatherTempUnit: StateFlow<String> = repository.weatherTempUnit
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "CELSIUS")

    // --- DISPLAY & GESTURE PREFERENCE FLOWS ---
    val clockStyle: StateFlow<String> = repository.clockStyle
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "DIGITAL_BOLD")

    val quickLockEnabled: StateFlow<Boolean> = repository.quickLockEnabled
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)

    val hapticFeedbackEnabled: StateFlow<Boolean> = repository.hapticFeedbackEnabled
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)

    val rotationEnabled: StateFlow<Boolean> = repository.rotationEnabled
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)

    val showBatteryPercentage: StateFlow<Boolean> = repository.showBatteryPercentage
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)

    // --- CASCADIA FAB ACTION PREFERENCE FLOWS ---
    val fabTapActionType: StateFlow<String> = repository.fabTapActionType
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "DIAL")

    val fabTapPackage: StateFlow<String?> = repository.fabTapPackage
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { updateBatteryState(it) }
        }
    }

    init {
        refreshApps()
        loadHiddenPackages()
        registerBatteryReceiver()
        checkDefaultLauncherStatus()
        refreshPermissions()
        fetchLiveWeatherForCurrentMode()
        loadSelectedMusicPackages()
    }

    // --- DISPLAY & GESTURE SETTERS ---
    fun setClockStyle(style: String) = viewModelScope.launch { repository.setClockStyle(style) }
    fun setAccentColor(colorArgb: Int) = viewModelScope.launch { repository.setAccentColor(colorArgb) }
    fun setQuickLockEnabled(enabled: Boolean) = viewModelScope.launch { repository.setQuickLockEnabled(enabled) }
    fun setHapticFeedbackEnabled(enabled: Boolean) = viewModelScope.launch { repository.setHapticFeedbackEnabled(enabled) }
    fun setRotationEnabled(enabled: Boolean) = viewModelScope.launch { repository.setRotationEnabled(enabled) }
    fun setShowBatteryPercentage(show: Boolean) = viewModelScope.launch { repository.setShowBatteryPercentage(show) }

    fun setFabTapAction(actionType: String, packageName: String? = null) {
        viewModelScope.launch {
            repository.setFabTapAction(actionType, packageName)
        }
    }

    // --- PERSISTENT POP-UP FOLDER MAPPING ---
    fun getFolderSubApps(parentPackage: String): List<AppModel> {
        val savedPackages = prefs.getStringSet("folder_sub_apps_$parentPackage", null)
        return if (!savedPackages.isNullOrEmpty()) {
            _apps.value.filter { savedPackages.contains(it.packageName) }
        } else {
            val visible = _apps.value.filter { !_hiddenPackages.value.contains(it.packageName) }
            val parentApp = visible.firstOrNull { it.packageName == parentPackage }
            val categoryMatch = if (parentApp != null) {
                visible.filter {
                    it.packageName != parentPackage && it.label.take(3).equals(parentApp.label.take(3), ignoreCase = true)
                }
            } else emptyList()

            if (categoryMatch.isNotEmpty()) categoryMatch.take(4) else visible.filter { it.packageName != parentPackage }.take(4)
        }
    }

    fun setFolderSubApps(parentPackage: String, subPackages: Set<String>) {
        prefs.edit().putStringSet("folder_sub_apps_$parentPackage", subPackages).apply()
    }

    // --- CALENDAR AGENDA MANAGEMENT ---
    fun loadCalendarEvents() {
        viewModelScope.launch {
            _upcomingEvents.value = calendarRepository.getUpcomingEvents()
        }
    }

    fun launchCalendarNewEvent(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = android.provider.CalendarContract.Events.CONTENT_URI
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openCalendarEvent(context: Context, eventId: Long) {
        try {
            val uri = android.content.ContentUris.withAppendedId(android.provider.CalendarContract.Events.CONTENT_URI, eventId)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = uri
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- USAGE BREAKER & LAUNCH TRACKING ---
    fun getAppLaunchCountToday(packageName: String): Int {
        val todayDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        return prefs.getInt("launch_${packageName}_$todayDate", 0)
    }

    fun incrementAppLaunchCount(packageName: String): Boolean {
        val todayDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val key = "launch_${packageName}_$todayDate"
        val count = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, count).apply()

        trackAppLaunch(packageName)
        return count > _usageBreakerLimit.value
    }

    fun trackAppLaunch(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val countKey = "launch_count_$packageName"
            val currentCount = prefs.getInt(countKey, 0)
            prefs.edit().putInt(countKey, currentCount + 1).apply()
        }
    }

    fun getRecentlyInstalledApps(): List<AppModel> {
        val pm = context.packageManager
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        return _apps.value.filter { app ->
            try {
                val packageInfo = pm.getPackageInfo(app.packageName, 0)
                packageInfo.firstInstallTime >= thirtyDaysAgo
            } catch (e: Exception) {
                false
            }
        }.sortedByDescending { app ->
            try { pm.getPackageInfo(app.packageName, 0).firstInstallTime } catch (e: Exception) { 0L }
        }
    }

    // --- REAL WEB SEARCH EXECUTOR ---
    fun launchWebSearch(context: Context, query: String) {
        try {
            val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- HIDDEN APPS MANAGEMENT ---
    private fun loadHiddenPackages() {
        val saved = prefs.getStringSet("hidden_packages", emptySet()) ?: emptySet()
        _hiddenPackages.value = saved
    }

    fun toggleHidePackage(packageName: String) {
        val current = _hiddenPackages.value.toMutableSet()
        if (current.contains(packageName)) current.remove(packageName) else current.add(packageName)
        _hiddenPackages.value = current
        prefs.edit().putStringSet("hidden_packages", current).apply()
    }

    // --- BACKUP & RESTORE LOGIC ---
    fun exportBackupJson(context: Context, uri: Uri): Boolean {
        return try {
            val json = JSONObject().apply {
                put("favoritePackages", JSONArray(favoritePackages.value.toList()))
                put("hiddenPackages", JSONArray(_hiddenPackages.value.toList()))
                put("widgetIds", JSONArray(widgetIds.value))
                put("homeOffsetY", homeOffsetY.value.toDouble())
                put("weatherEnabled", weatherEnabled.value)
                put("weatherLocationMode", weatherLocationMode.value)
                put("weatherCityName", weatherCityName.value)
                put("weatherTempUnit", weatherTempUnit.value)
                put("clockStyle", clockStyle.value)
                put("accentColor", accentColorArgb.value)
                put("quickLockEnabled", quickLockEnabled.value)
                put("hapticFeedbackEnabled", hapticFeedbackEnabled.value)
                put("rotationEnabled", rotationEnabled.value)
                put("showBatteryPercentage", showBatteryPercentage.value)
                put("fabTapActionType", fabTapActionType.value)
                put("fabTapPackage", fabTapPackage.value ?: "")
            }
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toString(4).toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importBackupJson(context: Context, uri: Uri): Boolean {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return false
            val json = JSONObject(jsonString)

            if (json.has("favoritePackages")) {
                val favArray = json.getJSONArray("favoritePackages")
                val favs = mutableListOf<String>()
                for (i in 0 until favArray.length()) favs.add(favArray.getString(i))
                viewModelScope.launch { repository.setFavorites(favs) }
            }

            if (json.has("hiddenPackages")) {
                val hiddenArray = json.getJSONArray("hiddenPackages")
                val hiddens = mutableSetOf<String>()
                for (i in 0 until hiddenArray.length()) hiddens.add(hiddenArray.getString(i))
                _hiddenPackages.value = hiddens
                prefs.edit().putStringSet("hidden_packages", hiddens).apply()
            }

            if (json.has("weatherEnabled")) setWeatherEnabled(json.getBoolean("weatherEnabled"))
            if (json.has("weatherCityName")) setWeatherCityName(json.getString("weatherCityName"))
            if (json.has("homeOffsetY")) updateHomeOffsetY(json.getDouble("homeOffsetY").toFloat())
            if (json.has("clockStyle")) setClockStyle(json.getString("clockStyle"))
            if (json.has("accentColor")) setAccentColor(json.getInt("accentColor"))
            if (json.has("quickLockEnabled")) setQuickLockEnabled(json.getBoolean("quickLockEnabled"))
            if (json.has("hapticFeedbackEnabled")) setHapticFeedbackEnabled(json.getBoolean("hapticFeedbackEnabled"))
            if (json.has("rotationEnabled")) setRotationEnabled(json.getBoolean("rotationEnabled"))
            if (json.has("showBatteryPercentage")) setShowBatteryPercentage(json.getBoolean("showBatteryPercentage"))
            if (json.has("fabTapActionType")) {
                val pkg = if (json.has("fabTapPackage")) json.getString("fabTapPackage").ifEmpty { null } else null
                setFabTapAction(json.getString("fabTapActionType"), pkg)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- MUSIC APP MANAGEMENT ---
    private fun loadSelectedMusicPackages() {
        val saved = prefs.getStringSet("selected_music_packages", setOf("com.spotify.music", "com.google.android.apps.youtube.music")) ?: emptySet()
        _selectedMusicPackages.value = saved
    }

    fun toggleMusicPackage(packageName: String) {
        val current = _selectedMusicPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedMusicPackages.value = current
        prefs.edit().putStringSet("selected_music_packages", current).apply()
    }

    fun getAutoDetectedMusicApps(): List<AppModel> {
        val pm = context.packageManager
        return _apps.value.filter { app ->
            try {
                val appInfo = pm.getApplicationInfo(app.packageName, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appInfo.category == ApplicationInfo.CATEGORY_AUDIO
                } else {
                    app.label.contains("Music", ignoreCase = true) ||
                    app.label.contains("Spotify", ignoreCase = true) ||
                    app.label.contains("Podcast", ignoreCase = true) ||
                    app.label.contains("Audio", ignoreCase = true)
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    fun fetchLiveWeatherForCurrentMode() {
        viewModelScope.launch {
            if (weatherLocationMode.value == "AUTO") {
                scanAndFetchLocationWeather()
            } else {
                val data = weatherRepository.fetchLiveWeather(22.3072, 73.1812)
                if (data != null) {
                    _liveWeather.value = data.copy(cityName = weatherCityName.value)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun scanAndFetchLocationWeather(): String = withContext(Dispatchers.IO) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        var detectedCity = "Vadodara"

        try {
            val lastKnownLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            if (lastKnownLocation != null) {
                val data = weatherRepository.fetchLiveWeather(lastKnownLocation.latitude, lastKnownLocation.longitude)
                if (data != null) {
                    _liveWeather.value = data
                    detectedCity = data.cityName
                    setWeatherCityName(data.cityName)
                }
            } else {
                val data = weatherRepository.fetchLiveWeather(22.3072, 73.1812)
                if (data != null) {
                    _liveWeather.value = data
                    detectedCity = data.cityName
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        detectedCity
    }

    fun refreshPermissions() {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        _notificationPermissionGranted.value = flat != null && flat.contains(packageName)
    }

    fun checkDefaultLauncherStatus() {
        val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true
        } else {
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
            val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName == context.packageName
        }
        _isDefaultLauncher.value = isDefault
    }

    fun dismissBannerForSession() {
        _isBannerDismissedSession.value = true
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = context.registerReceiver(batteryReceiver, filter)
        stickyIntent?.let { updateBatteryState(it) }
    }

    private fun updateBatteryState(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        if (level != -1 && scale != -1) {
            val pct = (level * 100 / scale.toFloat()).toInt()
            _batteryStatus.value = if (isCharging) "⚡ $pct%" else "$pct%"
        }
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }

            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }

            val installedApps = resolveInfos.mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName == context.packageName) return@mapNotNull null
                AppModel(
                    packageName = packageName,
                    label = resolveInfo.loadLabel(pm).toString(),
                    icon = resolveInfo.loadIcon(pm)
                )
            }.sortedBy { it.label.lowercase() }

            withContext(Dispatchers.Main) { _apps.value = installedApps }
        }
    }

    fun toggleFavorite(packageName: String) {
        viewModelScope.launch {
            val currentFavs = favoritePackages.value.toMutableSet()
            if (currentFavs.contains(packageName)) currentFavs.remove(packageName) else currentFavs.add(packageName)
            repository.setFavorites(currentFavs.toList())
        }
    }

    fun updateHomeOffsetY(offsetY: Float) = viewModelScope.launch { repository.setHomeOffsetY(offsetY) }
    fun updateWidgetHeight(height: Float) = viewModelScope.launch { repository.setWidgetHeight(height) }
    fun addWidgetId(widgetId: Int) = viewModelScope.launch { repository.addWidgetId(widgetId) }
    fun removeWidgetId(widgetId: Int) = viewModelScope.launch { repository.removeWidgetId(widgetId) }
    fun setShowMediaWidget(show: Boolean) = viewModelScope.launch { repository.setShowMediaWidget(show) }
    fun setMediaWidgetTheme(theme: MediaWidgetTheme) = viewModelScope.launch { repository.setMediaWidgetThemeName(theme.name) }

    fun setWeatherEnabled(enabled: Boolean) = viewModelScope.launch { repository.setWeatherEnabled(enabled) }
    fun setWeatherLocationMode(mode: String) = viewModelScope.launch { repository.setWeatherLocationMode(mode) }
    fun setWeatherCityName(city: String) = viewModelScope.launch { repository.setWeatherCityName(city) }
    fun setWeatherTempUnit(unit: String) = viewModelScope.launch { repository.setWeatherTempUnit(unit) }

    override fun onCleared() {
        super.onCleared()
        try { context.unregisterReceiver(batteryReceiver) } catch (e: Exception) {}
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LauncherViewModel::class.java)) {
                val repository = SettingsRepository(context.applicationContext)
                return LauncherViewModel(repository, context.applicationContext) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
