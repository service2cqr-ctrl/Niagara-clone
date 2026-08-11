package com.niagaraclone.launcher.data

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.provider.CalendarContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.niagaraclone.launcher.services.NotificationSummaryModel
import com.niagaraclone.launcher.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class MediaWidgetTheme { MATCH_ACCENT, ALBUM_ART, DARK, FROSTED_GLASS, LIGHT }

data class LiveWeatherModel(
    val temperatureCelsius: Float = 28f,
    val conditionIcon: String = "☀️"
)

class SettingsRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    // DataStore Keys
    private val APP_THEME_KEY = stringPreferencesKey("app_theme")
    private val CLOCK_STYLE_KEY = stringPreferencesKey("clock_style")
    private val ACCENT_COLOR_KEY = intPreferencesKey("accent_color")
    private val HOME_OFFSET_Y_KEY = floatPreferencesKey("home_offset_y")
    private val WIDGET_HEIGHT_KEY = intPreferencesKey("widget_height")
    private val WIDGET_IDS_KEY = stringSetPreferencesKey("widget_ids")
    private val WEATHER_ENABLED_KEY = booleanPreferencesKey("weather_enabled")
    private val WEATHER_TEMP_UNIT_KEY = stringPreferencesKey("weather_temp_unit")
    private val SHOW_MEDIA_WIDGET_KEY = booleanPreferencesKey("show_media_widget")
    private val MEDIA_WIDGET_THEME_KEY = stringPreferencesKey("media_widget_theme")
    private val QUICK_LOCK_KEY = booleanPreferencesKey("quick_lock_enabled")
    private val SHOW_BATTERY_KEY = booleanPreferencesKey("show_battery_percentage")
    private val CASCADIA_BTN_KEY = booleanPreferencesKey("cascadia_button_enabled")
    private val CALENDAR_PREVIEW_KEY = booleanPreferencesKey("calendar_preview_enabled")
    private val FAB_TAP_ACTION_KEY = stringPreferencesKey("fab_tap_action")
    private val FAB_TAP_PKG_KEY = stringPreferencesKey("fab_tap_package")
    private val FAB_SWIPE_ACTION_KEY = stringPreferencesKey("fab_swipe_action")
    private val FAB_SWIPE_PKG_KEY = stringPreferencesKey("fab_swipe_package")
    private val HIDDEN_PACKAGES_KEY = stringSetPreferencesKey("hidden_packages")
    private val FAVORITE_PACKAGES_KEY = stringSetPreferencesKey("favorite_packages")
    private val MUSIC_PACKAGES_KEY = stringSetPreferencesKey("music_packages")
    private val ICON_PACK_KEY = stringPreferencesKey("selected_icon_pack")
    private val HAPTIC_ENABLED_KEY = booleanPreferencesKey("haptic_feedback_enabled")
    private val ROTATION_ENABLED_KEY = booleanPreferencesKey("rotation_enabled")
    private val CITY_NAME_KEY = stringPreferencesKey("weather_city_name")
    private val LOCATION_MODE_KEY = stringPreferencesKey("weather_location_mode")
    private val SEARCH_CONTACTS_KEY = booleanPreferencesKey("search_contacts_enabled")

    // StateFlow Exposures
    val appTheme: StateFlow<AppTheme> = context.dataStore.data.map {
        when (it[APP_THEME_KEY]) {
            "LIGHT" -> AppTheme.LIGHT
            "DARK" -> AppTheme.DARK
            else -> AppTheme.AUTO
        }
    }.stateIn(scope, SharingStarted.Eagerly, AppTheme.AUTO)

    val clockStyle: StateFlow<String> = context.dataStore.data.map { it[CLOCK_STYLE_KEY] ?: "SPLIT_COLORS" }.stateIn(scope, SharingStarted.Eagerly, "SPLIT_COLORS")
    val accentColorArgb: StateFlow<Int> = context.dataStore.data.map { it[ACCENT_COLOR_KEY] ?: 0xFF4DD0E1.toInt() }.stateIn(scope, SharingStarted.Eagerly, 0xFF4DD0E1.toInt())
    val homeOffsetY: StateFlow<Float> = context.dataStore.data.map { it[HOME_OFFSET_Y_KEY] ?: 0f }.stateIn(scope, SharingStarted.Eagerly, 0f)
    val widgetHeight: StateFlow<Int> = context.dataStore.data.map { it[WIDGET_HEIGHT_KEY] ?: 180 }.stateIn(scope, SharingStarted.Eagerly, 180)
    val widgetIds: StateFlow<Set<Int>> = context.dataStore.data.map { (it[WIDGET_IDS_KEY] ?: emptySet()).mapNotNull { id -> id.toIntOrNull() }.toSet() }.stateIn(scope, SharingStarted.Eagerly, emptySet())
    val weatherEnabled: StateFlow<Boolean> = context.dataStore.data.map { it[WEATHER_ENABLED_KEY] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)
    val weatherTempUnit: StateFlow<String> = context.dataStore.data.map { it[WEATHER_TEMP_UNIT_KEY] ?: "CELSIUS" }.stateIn(scope, SharingStarted.Eagerly, "CELSIUS")
    val showMediaWidget: StateFlow<Boolean> = context.dataStore.data.map { it[SHOW_MEDIA_WIDGET_KEY] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)
    val mediaWidgetTheme: StateFlow<MediaWidgetTheme> = context.dataStore.data.map {
        when (it[MEDIA_WIDGET_THEME_KEY]) {
            "LIGHT" -> MediaWidgetTheme.LIGHT
            "DARK" -> MediaWidgetTheme.DARK
            "MATCH_ACCENT" -> MediaWidgetTheme.MATCH_ACCENT
            "ALBUM_ART" -> MediaWidgetTheme.ALBUM_ART
            else -> MediaWidgetTheme.FROSTED_GLASS
        }
    }.stateIn(scope, SharingStarted.Eagerly, MediaWidgetTheme.FROSTED_GLASS)

    val quickLockEnabled: StateFlow<Boolean> = context.dataStore.data.map { it[QUICK_LOCK_KEY] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)
    val showBatteryPercentage: StateFlow<Boolean> = context.dataStore.data.map { it[SHOW_BATTERY_KEY] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)
    val cascadiaButtonEnabled: StateFlow<Boolean> = context.dataStore.data.map { it[CASCADIA_BTN_KEY] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)
    val calendarPreviewEnabled: StateFlow<Boolean> = context.dataStore.data.map { it[CALENDAR_PREVIEW_KEY] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)
    val searchContactsEnabled: StateFlow<Boolean> = context.dataStore.data.map { it[SEARCH_CONTACTS_KEY] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)

    val fabTapActionType: StateFlow<String> = context.dataStore.data.map { it[FAB_TAP_ACTION_KEY] ?: "PHONE" }.stateIn(scope, SharingStarted.Eagerly, "PHONE")
    val fabTapPackage: StateFlow<String> = context.dataStore.data.map { it[FAB_TAP_PKG_KEY] ?: "" }.stateIn(scope, SharingStarted.Eagerly, "")
    val fabSwipeActionType: StateFlow<String> = context.dataStore.data.map { it[FAB_SWIPE_ACTION_KEY] ?: "SEARCH" }.stateIn(scope, SharingStarted.Eagerly, "SEARCH")
    val fabSwipePackage: StateFlow<String> = context.dataStore.data.map { it[FAB_SWIPE_PKG_KEY] ?: "" }.stateIn(scope, SharingStarted.Eagerly, "")

    val hiddenPackages: StateFlow<Set<String>> = context.dataStore.data.map { it[HIDDEN_PACKAGES_KEY] ?: emptySet() }.stateIn(scope, SharingStarted.Eagerly, emptySet())
    val favoritePackages: StateFlow<Set<String>> = context.dataStore.data.map { it[FAVORITE_PACKAGES_KEY] ?: emptySet() }.stateIn(scope, SharingStarted.Eagerly, emptySet())
    val selectedMusicPackages: StateFlow<Set<String>> = context.dataStore.data.map { it[MUSIC_PACKAGES_KEY] ?: emptySet() }.stateIn(scope, SharingStarted.Eagerly, emptySet())
    val selectedIconPack: StateFlow<String> = context.dataStore.data.map { it[ICON_PACK_KEY] ?: "SYSTEM" }.stateIn(scope, SharingStarted.Eagerly, "SYSTEM")
    val hapticFeedbackEnabled: StateFlow<Boolean> = context.dataStore.data.map { it[HAPTIC_ENABLED_KEY] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)
    val rotationEnabled: StateFlow<Boolean> = context.dataStore.data.map { it[ROTATION_ENABLED_KEY] ?: false }.stateIn(scope, SharingStarted.Eagerly, false)

    val weatherCityName: StateFlow<String> = context.dataStore.data.map { it[CITY_NAME_KEY] ?: "Pune" }.stateIn(scope, SharingStarted.Eagerly, "Pune")
    val weatherLocationMode: StateFlow<String> = context.dataStore.data.map { it[LOCATION_MODE_KEY] ?: "GPS" }.stateIn(scope, SharingStarted.Eagerly, "GPS")

    private val _batteryStatus = MutableStateFlow("85%")
    val batteryStatus: StateFlow<String> = _batteryStatus.asStateFlow()

    private val _isDefaultLauncher = MutableStateFlow(false)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    private val _notificationPermissionGranted = MutableStateFlow(false)
    val notificationPermissionGranted: StateFlow<Boolean> = _notificationPermissionGranted.asStateFlow()

    private val _liveWeather = MutableStateFlow<LiveWeatherModel?>(LiveWeatherModel(28f, "☀️"))
    val liveWeather: StateFlow<LiveWeatherModel?> = _liveWeather.asStateFlow()

    private val _notificationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val notificationCounts: StateFlow<Map<String, Int>> = _notificationCounts.asStateFlow()

    private val _upcomingEvents = MutableStateFlow<List<CalendarEventModel>>(emptyList())
    val upcomingEvents: StateFlow<List<CalendarEventModel>> = _upcomingEvents.asStateFlow()

    private val _notificationSummaries = MutableStateFlow<List<NotificationSummaryModel>>(emptyList())
    val notificationSummaries: StateFlow<List<NotificationSummaryModel>> = _notificationSummaries.asStateFlow()

    init {
        checkBatteryStatus()
        checkNotificationPermission()
        checkDefaultLauncher()
    }

    fun checkBatteryStatus() {
        try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            _batteryStatus.value = "$level%"
        } catch (e: Exception) {
            _batteryStatus.value = "100%"
        }
    }

    fun checkNotificationPermission() {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
        _notificationPermissionGranted.value = flat.contains(context.packageName)
    }

    fun checkDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            _isDefaultLauncher.value = roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true
        } else {
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
            val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            _isDefaultLauncher.value = resolveInfo?.activityInfo?.packageName == context.packageName
        }
    }

    suspend fun setAppTheme(theme: AppTheme) { context.dataStore.edit { it[APP_THEME_KEY] = theme.name } }
    suspend fun setClockStyle(style: String) { context.dataStore.edit { it[CLOCK_STYLE_KEY] = style } }
    suspend fun setAccentColorArgb(color: Int) { context.dataStore.edit { it[ACCENT_COLOR_KEY] = color } }
    suspend fun setHomeOffsetY(offset: Float) { context.dataStore.edit { it[HOME_OFFSET_Y_KEY] = offset } }
    suspend fun addWidgetId(id: Int) { context.dataStore.edit { val set = (it[WIDGET_IDS_KEY] ?: emptySet()).toMutableSet(); set.add(id.toString()); it[WIDGET_IDS_KEY] = set } }
    suspend fun removeWidgetId(id: Int) { context.dataStore.edit { val set = (it[WIDGET_IDS_KEY] ?: emptySet()).toMutableSet(); set.remove(id.toString()); it[WIDGET_IDS_KEY] = set } }
    suspend fun setWeatherEnabled(enabled: Boolean) { context.dataStore.edit { it[WEATHER_ENABLED_KEY] = enabled } }
    suspend fun setWeatherTempUnit(unit: String) { context.dataStore.edit { it[WEATHER_TEMP_UNIT_KEY] = unit } }
    suspend fun setShowMediaWidget(show: Boolean) { context.dataStore.edit { it[SHOW_MEDIA_WIDGET_KEY] = show } }
    suspend fun setMediaWidgetTheme(theme: MediaWidgetTheme) { context.dataStore.edit { it[MEDIA_WIDGET_THEME_KEY] = theme.name } }
    suspend fun setQuickLockEnabled(enabled: Boolean) { context.dataStore.edit { it[QUICK_LOCK_KEY] = enabled } }
    suspend fun setShowBatteryPercentage(show: Boolean) { context.dataStore.edit { it[SHOW_BATTERY_KEY] = show } }
    suspend fun setCascadiaButtonEnabled(enabled: Boolean) { context.dataStore.edit { it[CASCADIA_BTN_KEY] = enabled } }
    suspend fun setCalendarPreviewEnabled(enabled: Boolean) { context.dataStore.edit { it[CALENDAR_PREVIEW_KEY] = enabled } }
    suspend fun setSearchContactsEnabled(enabled: Boolean) { context.dataStore.edit { it[SEARCH_CONTACTS_KEY] = enabled } }
    suspend fun setFabTapAction(actionType: String, pkg: String = "") { context.dataStore.edit { it[FAB_TAP_ACTION_KEY] = actionType; it[FAB_TAP_PKG_KEY] = pkg } }
    suspend fun setFabSwipeAction(actionType: String, pkg: String = "") { context.dataStore.edit { it[FAB_SWIPE_ACTION_KEY] = actionType; it[FAB_SWIPE_PKG_KEY] = pkg } }

    suspend fun toggleFavorite(pkg: String) {
        context.dataStore.edit {
            val set = (it[FAVORITE_PACKAGES_KEY] ?: emptySet()).toMutableSet()
            if (set.contains(pkg)) set.remove(pkg) else set.add(pkg)
            it[FAVORITE_PACKAGES_KEY] = set
        }
    }

    suspend fun toggleHidePackage(pkg: String) {
        context.dataStore.edit {
            val set = (it[HIDDEN_PACKAGES_KEY] ?: emptySet()).toMutableSet()
            if (set.contains(pkg)) set.remove(pkg) else set.add(pkg)
            it[HIDDEN_PACKAGES_KEY] = set
        }
    }

    suspend fun toggleMusicPackage(pkg: String) {
        context.dataStore.edit {
            val set = (it[MUSIC_PACKAGES_KEY] ?: emptySet()).toMutableSet()
            if (set.contains(pkg)) set.remove(pkg) else set.add(pkg)
            it[MUSIC_PACKAGES_KEY] = set
        }
    }

    suspend fun setSelectedIconPack(pack: String) { context.dataStore.edit { it[ICON_PACK_KEY] = pack } }
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) { context.dataStore.edit { it[HAPTIC_ENABLED_KEY] = enabled } }
    suspend fun setRotationEnabled(enabled: Boolean) { context.dataStore.edit { it[ROTATION_ENABLED_KEY] = enabled } }
    suspend fun setWeatherCityName(name: String) { context.dataStore.edit { it[CITY_NAME_KEY] = name } }
    suspend fun setWeatherLocationMode(mode: String) { context.dataStore.edit { it[LOCATION_MODE_KEY] = mode } }

    fun loadCalendarEvents() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            try {
                val events = mutableListOf<CalendarEventModel>()
                val cursor = context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    arrayOf(CalendarContract.Events._ID, CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART, CalendarContract.Events.DTEND, CalendarContract.Events.ALL_DAY),
                    "${CalendarContract.Events.DTSTART} >= ?",
                    arrayOf(System.currentTimeMillis().toString()),
                    "${CalendarContract.Events.DTSTART} ASC"
                )
                cursor?.use {
                    val idIdx = it.getColumnIndex(CalendarContract.Events._ID)
                    val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE)
                    val startIdx = it.getColumnIndex(CalendarContract.Events.DTSTART)
                    val endIdx = it.getColumnIndex(CalendarContract.Events.DTEND)
                    val allDayIdx = it.getColumnIndex(CalendarContract.Events.ALL_DAY)

                    while (it.moveToNext() && events.size < 5) {
                        events.add(
                            CalendarEventModel(
                                id = it.getLong(idIdx),
                                title = it.getString(titleIdx) ?: "Event",
                                startTimeMillis = it.getLong(startIdx),
                                endTimeMillis = if (endIdx != -1) it.getLong(endIdx) else 0L,
                                isAllDay = it.getInt(allDayIdx) == 1
                            )
                        )
                    }
                }
                _upcomingEvents.value = events
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getFolderSubAppPackages(parentPackage: String): Set<String> {
        val prefs = context.getSharedPreferences("popUpFolders", Context.MODE_PRIVATE)
        return prefs.getStringSet(parentPackage, emptySet()) ?: emptySet()
    }

    suspend fun setFolderSubApps(parentPackage: String, subPackages: Set<String>) {
        val prefs = context.getSharedPreferences("popUpFolders", Context.MODE_PRIVATE)
        prefs.edit().putStringSet(parentPackage, subPackages).apply()
    }

    fun exportBackupJson(): String = JSONObject().put("version", 1).toString()
    fun importBackupJson(jsonStr: String): Boolean = try { JSONObject(jsonStr); true } catch (e: Exception) { false }
}
