package com.niagaraclone.launcher.ui

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.data.AppTheme
import com.niagaraclone.launcher.data.SettingsRepository
import com.niagaraclone.launcher.data.WeatherData
import com.niagaraclone.launcher.data.WeatherRepository
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

class LauncherViewModel(
    val repository: SettingsRepository,
    private val context: Context
) : ViewModel() {

    private val weatherRepository = WeatherRepository(context)

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

    // --- LIVE WEATHER STATES ---
    private val _liveWeather = MutableStateFlow<WeatherData?>(null)
    val liveWeather: StateFlow<WeatherData?> = _liveWeather.asStateFlow()

    val theme: StateFlow<AppTheme> = repository.theme
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = AppTheme.SYSTEM)

    val accentColorArgb: StateFlow<Int> = repository.accentColor
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0xFF3B82F6.toInt())

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

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { updateBatteryState(it) }
        }
    }

    init {
        refreshApps()
        registerBatteryReceiver()
        checkDefaultLauncherStatus()
        refreshPermissions()
        fetchLiveWeatherForCurrentMode()
    }

    fun fetchLiveWeatherForCurrentMode() {
        viewModelScope.launch {
            if (weatherLocationMode.value == "AUTO") {
                scanAndFetchLocationWeather()
            } else {
                // Default fallback coords for fixed city
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
                // Default coordinates if location unavailable
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
