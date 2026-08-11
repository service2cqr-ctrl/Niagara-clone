package com.niagaraclone.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

enum class AppTheme { LIGHT, DARK, SYSTEM }

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_ACCENT_COLOR = intPreferencesKey("accent_color")
        val KEY_FAVORITES = stringPreferencesKey("favorites")
        val KEY_HIDDEN_APPS = stringPreferencesKey("hidden_apps")
        val KEY_HOME_OFFSET_Y = floatPreferencesKey("home_offset_y")
        val KEY_WIDGET_HEIGHT = floatPreferencesKey("widget_height")
        val KEY_WIDGET_IDS = stringPreferencesKey("widget_ids_list")

        // Media Widget Keys
        val KEY_SHOW_MEDIA_WIDGET = booleanPreferencesKey("show_media_widget")
        val KEY_MEDIA_WIDGET_THEME = stringPreferencesKey("media_widget_theme")

        // Weather Keys
        val KEY_WEATHER_ENABLED = booleanPreferencesKey("weather_enabled")
        val KEY_WEATHER_LOCATION_MODE = stringPreferencesKey("weather_location_mode")
        val KEY_WEATHER_CITY_NAME = stringPreferencesKey("weather_city_name")
        val KEY_WEATHER_TEMP_UNIT = stringPreferencesKey("weather_temp_unit")

        // Display & Gesture Keys
        val KEY_CLOCK_STYLE = stringPreferencesKey("clock_style")
        val KEY_QUICK_LOCK_ENABLED = booleanPreferencesKey("quick_lock_enabled")
        val KEY_HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val KEY_ROTATION_ENABLED = booleanPreferencesKey("rotation_enabled")
        val KEY_SHOW_BATTERY_PERCENTAGE = booleanPreferencesKey("show_battery_percentage")

        // Cascadia (FAB) Button Persistence Keys
        val KEY_CASCADIA_BUTTON_ENABLED = booleanPreferencesKey("cascadia_button_enabled")
        val KEY_FAB_TAP_ACTION_TYPE = stringPreferencesKey("fab_tap_action_type")
        val KEY_FAB_TAP_PACKAGE = stringPreferencesKey("fab_tap_package")
        val KEY_FAB_SWIPE_ACTION_TYPE = stringPreferencesKey("fab_swipe_action_type")
        val KEY_FAB_SWIPE_PACKAGE = stringPreferencesKey("fab_swipe_package")
    }

    val theme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME] ?: AppTheme.SYSTEM.name
        try { AppTheme.valueOf(raw) } catch (e: Exception) { AppTheme.SYSTEM }
    }

    val accentColor: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACCENT_COLOR] ?: 0xFF4DD0E1.toInt()
    }

    val favorites: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_FAVORITES] ?: ""
        if (raw.isBlank()) emptyList() else raw.split(",")
    }

    val hiddenApps: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_HIDDEN_APPS] ?: ""
        if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    val homeOffsetY: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_HOME_OFFSET_Y] ?: 0f
    }

    val widgetHeight: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_HEIGHT] ?: 180f
    }

    val widgetIds: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_WIDGET_IDS] ?: ""
        if (raw.isBlank()) emptyList()
        else raw.split(",").mapNotNull { it.toIntOrNull() }
    }

    val showMediaWidget: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_MEDIA_WIDGET] ?: true
    }

    val mediaWidgetThemeName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MEDIA_WIDGET_THEME] ?: "FROSTED_GLASS"
    }

    val weatherEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WEATHER_ENABLED] ?: true
    }

    val weatherLocationMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WEATHER_LOCATION_MODE] ?: "AUTO"
    }

    val weatherCityName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WEATHER_CITY_NAME] ?: "Vadodara"
    }

    val weatherTempUnit: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WEATHER_TEMP_UNIT] ?: "CELSIUS"
    }

    val clockStyle: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CLOCK_STYLE] ?: "DIGITAL_BOLD"
    }

    val quickLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_QUICK_LOCK_ENABLED] ?: true
    }

    val hapticFeedbackEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAPTIC_FEEDBACK_ENABLED] ?: true
    }

    val rotationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ROTATION_ENABLED] ?: false
    }

    val showBatteryPercentage: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_BATTERY_PERCENTAGE] ?: true
    }

    val cascadiaButtonEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CASCADIA_BUTTON_ENABLED] ?: true
    }

    val fabTapActionType: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAB_TAP_ACTION_TYPE] ?: "DIAL"
    }

    val fabTapPackage: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAB_TAP_PACKAGE]
    }

    val fabSwipeActionType: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAB_SWIPE_ACTION_TYPE] ?: "SEARCH"
    }

    val fabSwipePackage: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAB_SWIPE_PACKAGE]
    }

    suspend fun setTheme(theme: AppTheme) { context.dataStore.edit { prefs -> prefs[KEY_THEME] = theme.name } }
    suspend fun setAccentColor(colorArgb: Int) { context.dataStore.edit { prefs -> prefs[KEY_ACCENT_COLOR] = colorArgb } }
    suspend fun setFavorites(packages: List<String>) { context.dataStore.edit { prefs -> prefs[KEY_FAVORITES] = packages.joinToString(",") } }
    suspend fun setHiddenApps(packages: Set<String>) { context.dataStore.edit { prefs -> prefs[KEY_HIDDEN_APPS] = packages.joinToString(",") } }
    suspend fun setHomeOffsetY(offsetY: Float) { context.dataStore.edit { prefs -> prefs[KEY_HOME_OFFSET_Y] = offsetY } }
    suspend fun setWidgetHeight(height: Float) { context.dataStore.edit { prefs -> prefs[KEY_WIDGET_HEIGHT] = height } }
    suspend fun setShowMediaWidget(show: Boolean) { context.dataStore.edit { prefs -> prefs[KEY_SHOW_MEDIA_WIDGET] = show } }
    suspend fun setMediaWidgetThemeName(themeName: String) { context.dataStore.edit { prefs -> prefs[KEY_MEDIA_WIDGET_THEME] = themeName } }
    suspend fun setWeatherEnabled(enabled: Boolean) { context.dataStore.edit { prefs -> prefs[KEY_WEATHER_ENABLED] = enabled } }
    suspend fun setWeatherLocationMode(mode: String) { context.dataStore.edit { prefs -> prefs[KEY_WEATHER_LOCATION_MODE] = mode } }
    suspend fun setWeatherCityName(cityName: String) { context.dataStore.edit { prefs -> prefs[KEY_WEATHER_CITY_NAME] = cityName } }
    suspend fun setWeatherTempUnit(unit: String) { context.dataStore.edit { prefs -> prefs[KEY_WEATHER_TEMP_UNIT] = unit } }
    suspend fun setClockStyle(style: String) { context.dataStore.edit { prefs -> prefs[KEY_CLOCK_STYLE] = style } }
    suspend fun setQuickLockEnabled(enabled: Boolean) { context.dataStore.edit { prefs -> prefs[KEY_QUICK_LOCK_ENABLED] = enabled } }
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) { context.dataStore.edit { prefs -> prefs[KEY_HAPTIC_FEEDBACK_ENABLED] = enabled } }
    suspend fun setRotationEnabled(enabled: Boolean) { context.dataStore.edit { prefs -> prefs[KEY_ROTATION_ENABLED] = enabled } }
    suspend fun setShowBatteryPercentage(show: Boolean) { context.dataStore.edit { prefs -> prefs[KEY_SHOW_BATTERY_PERCENTAGE] = show } }
    suspend fun setCascadiaButtonEnabled(enabled: Boolean) { context.dataStore.edit { prefs -> prefs[KEY_CASCADIA_BUTTON_ENABLED] = enabled } }

    suspend fun setFabTapAction(actionType: String, packageName: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FAB_TAP_ACTION_TYPE] = actionType
            if (packageName != null) prefs[KEY_FAB_TAP_PACKAGE] = packageName else prefs.remove(KEY_FAB_TAP_PACKAGE)
        }
    }

    suspend fun setFabSwipeAction(actionType: String, packageName: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FAB_SWIPE_ACTION_TYPE] = actionType
            if (packageName != null) prefs[KEY_FAB_SWIPE_PACKAGE] = packageName else prefs.remove(KEY_FAB_SWIPE_PACKAGE)
        }
    }

    suspend fun addWidgetId(widgetId: Int) {
        context.dataStore.edit { prefs ->
            val currentList = prefs[KEY_WIDGET_IDS]?.split(",")?.mapNotNull { it.toIntOrNull() }?.toMutableList() ?: mutableListOf()
            if (currentList.size < 4 && !currentList.contains(widgetId)) {
                currentList.add(widgetId)
                prefs[KEY_WIDGET_IDS] = currentList.joinToString(",")
            }
        }
    }

    suspend fun removeWidgetId(widgetId: Int) {
        context.dataStore.edit { prefs ->
            val currentList = prefs[KEY_WIDGET_IDS]?.split(",")?.mapNotNull { it.toIntOrNull() }?.toMutableList() ?: mutableListOf()
            if (currentList.remove(widgetId)) {
                prefs[KEY_WIDGET_IDS] = currentList.joinToString(",")
            }
        }
    }
}
