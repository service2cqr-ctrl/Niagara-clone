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
        val KEY_ICON_PACK = stringPreferencesKey("icon_pack")
        val KEY_SWIPE_UP_APP = stringPreferencesKey("swipe_up_app")
        val KEY_SWIPE_DOWN_APP = stringPreferencesKey("swipe_down_app")
        val KEY_DOUBLE_TAP_APP = stringPreferencesKey("double_tap_app")
        val KEY_WALLPAPER_ZOOM = stringPreferencesKey("wallpaper_zoom")
        val KEY_FONT_FAMILY = stringPreferencesKey("font_family")
        val KEY_HOME_OFFSET_Y = floatPreferencesKey("home_offset_y")
        val KEY_WIDGET_HEIGHT = floatPreferencesKey("widget_height")
        val KEY_WIDGET_IDS = stringPreferencesKey("widget_ids_list")

        // Media Widget Persistence Keys
        val KEY_SHOW_MEDIA_WIDGET = booleanPreferencesKey("show_media_widget")
        val KEY_MEDIA_WIDGET_THEME = stringPreferencesKey("media_widget_theme")
    }

    val theme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME] ?: AppTheme.SYSTEM.name
        try { AppTheme.valueOf(raw) } catch (e: Exception) { AppTheme.SYSTEM }
    }

    val accentColor: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACCENT_COLOR] ?: 0xFF3B82F6.toInt()
    }

    val favorites: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_FAVORITES] ?: ""
        if (raw.isBlank()) emptyList() else raw.split(",")
    }

    val hiddenApps: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_HIDDEN_APPS] ?: ""
        if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    val iconPackPackage: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ICON_PACK]
    }

    val homeOffsetY: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_HOME_OFFSET_Y] ?: 0f
    }

    val widgetHeight: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_HEIGHT] ?: 180f
    }

    // --- WIDGET STACK FLOW ---
    val widgetIds: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_WIDGET_IDS] ?: ""
        if (raw.isBlank()) emptyList()
        else raw.split(",").mapNotNull { it.toIntOrNull() }
    }

    // --- MEDIA WIDGET FLOWS ---
    val showMediaWidget: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_MEDIA_WIDGET] ?: true
    }

    val mediaWidgetThemeName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MEDIA_WIDGET_THEME] ?: "FROSTED_GLASS"
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { prefs -> prefs[KEY_THEME] = theme.name }
    }

    suspend fun setAccentColor(colorArgb: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_ACCENT_COLOR] = colorArgb }
    }

    suspend fun setFavorites(packages: List<String>) {
        context.dataStore.edit { prefs -> prefs[KEY_FAVORITES] = packages.joinToString(",") }
    }

    suspend fun setHiddenApps(packages: Set<String>) {
        context.dataStore.edit { prefs -> prefs[KEY_HIDDEN_APPS] = packages.joinToString(",") }
    }

    suspend fun setIconPack(packageName: String?) {
        context.dataStore.edit { prefs ->
            if (packageName == null) prefs.remove(KEY_ICON_PACK) else prefs[KEY_ICON_PACK] = packageName
        }
    }

    suspend fun setHomeOffsetY(offsetY: Float) {
        context.dataStore.edit { prefs -> prefs[KEY_HOME_OFFSET_Y] = offsetY }
    }

    suspend fun setWidgetHeight(height: Float) {
        context.dataStore.edit { prefs -> prefs[KEY_WIDGET_HEIGHT] = height }
    }

    // --- MEDIA WIDGET SETTERS ---
    suspend fun setShowMediaWidget(show: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_SHOW_MEDIA_WIDGET] = show }
    }

    suspend fun setMediaWidgetThemeName(themeName: String) {
        context.dataStore.edit { prefs -> prefs[KEY_MEDIA_WIDGET_THEME] = themeName }
    }

    // --- WIDGET STACK HELPER METHODS ---
    suspend fun addWidgetId(widgetId: Int) {
        context.dataStore.edit { prefs ->
            val currentList = prefs[KEY_WIDGET_IDS]
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
                ?.toMutableList() ?: mutableListOf()

            // Enforce max 4 widgets in stack limit
            if (currentList.size < 4 && !currentList.contains(widgetId)) {
                currentList.add(widgetId)
                prefs[KEY_WIDGET_IDS] = currentList.joinToString(",")
            }
        }
    }

    suspend fun removeWidgetId(widgetId: Int) {
        context.dataStore.edit { prefs ->
            val currentList = prefs[KEY_WIDGET_IDS]
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
                ?.toMutableList() ?: mutableListOf()

            if (currentList.remove(widgetId)) {
                prefs[KEY_WIDGET_IDS] = currentList.joinToString(",")
            }
        }
    }

    suspend fun reorderWidgetIds(newList: List<Int>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WIDGET_IDS] = newList.take(4).joinToString(",")
        }
    }

    suspend fun snapshot(): Map<Preferences.Key<*>, Any> {
        return context.dataStore.data.first().asMap()
    }

    suspend fun restore(values: Map<String, String>) {
        context.dataStore.edit { prefs ->
            values.forEach { (keyName, valueStr) ->
                val key = stringPreferencesKey(keyName)
                prefs[key] = valueStr
            }
        }
    }
}
