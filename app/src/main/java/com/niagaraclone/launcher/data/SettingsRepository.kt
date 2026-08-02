package com.niagaraclone.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "launcher_settings")

enum class AppTheme { LIGHT, DARK, SYSTEM }

/** Which app/action a swipe or gesture triggers. */
enum class GestureAction { NONE, OPEN_NOTIFICATIONS, OPEN_WIDGETS, OPEN_APP_DRAWER, LAUNCH_APP }

data class GestureBinding(val action: GestureAction, val targetPackage: String? = null)

object Keys {
    val THEME = stringPreferencesKey("theme")
    val ACCENT_COLOR = intPreferencesKey("accent_color")
    val FAVORITES = stringPreferencesKey("favorites_csv")
    val ICON_PACK = stringPreferencesKey("icon_pack_package")
    val SWIPE_UP = stringPreferencesKey("gesture_swipe_up")
    val SWIPE_DOWN = stringPreferencesKey("gesture_swipe_down")
    val SWIPE_LEFT = stringPreferencesKey("gesture_swipe_left")
    val SWIPE_RIGHT = stringPreferencesKey("gesture_swipe_right")
    val DOUBLE_TAP = stringPreferencesKey("gesture_double_tap")
    val HIDDEN_APPS = stringPreferencesKey("hidden_apps_csv")
}

class SettingsRepository(private val context: Context) {

    val theme: Flow<AppTheme> = context.dataStore.data.map {
        AppTheme.valueOf(it[Keys.THEME] ?: AppTheme.SYSTEM.name)
    }

    val accentColor: Flow<Int> = context.dataStore.data.map {
        it[Keys.ACCENT_COLOR] ?: 0xFF3B82F6.toInt() // default blue accent, Niagara-style
    }

    val favorites: Flow<List<String>> = context.dataStore.data.map {
        it[Keys.FAVORITES]?.split(",")?.filter { p -> p.isNotBlank() } ?: emptyList()
    }

    val hiddenApps: Flow<List<String>> = context.dataStore.data.map {
        it[Keys.HIDDEN_APPS]?.split(",")?.filter { p -> p.isNotBlank() } ?: emptyList()
    }

    val iconPackPackage: Flow<String?> = context.dataStore.data.map { it[Keys.ICON_PACK] }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setAccentColor(colorArgb: Int) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = colorArgb }
    }

    suspend fun setFavorites(packages: List<String>) {
        context.dataStore.edit { it[Keys.FAVORITES] = packages.joinToString(",") }
    }

    suspend fun setHiddenApps(packages: List<String>) {
        context.dataStore.edit { it[Keys.HIDDEN_APPS] = packages.joinToString(",") }
    }

    suspend fun setIconPack(packageName: String?) {
        context.dataStore.edit {
            if (packageName == null) it.remove(Keys.ICON_PACK) else it[Keys.ICON_PACK] = packageName
        }
    }

    suspend fun setGesture(key: Preferences.Key<String>, binding: GestureBinding) {
        context.dataStore.edit {
            it[key] = "${binding.action.name}|${binding.targetPackage ?: ""}"
        }
    }

    fun gestureFlow(key: Preferences.Key<String>): Flow<GestureBinding> = context.dataStore.data.map { prefs ->
        val raw = prefs[key] ?: return@map GestureBinding(GestureAction.NONE)
        val parts = raw.split("|")
        val action = runCatching { GestureAction.valueOf(parts[0]) }.getOrDefault(GestureAction.NONE)
        val target = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
        GestureBinding(action, target)
    }

    /** Raw snapshot of every stored preference, used for JSON export in BackupManager. */
    suspend fun snapshot(): Map<Preferences.Key<*>, Any> {
        return kotlinx.coroutines.flow.first(context.dataStore.data.map { it.asMap() })
    }

    /** Restores preferences from a key/value map produced by [snapshot] (used by BackupManager). */
    suspend fun restore(values: Map<String, String>) {
        context.dataStore.edit { prefs ->
            values.forEach { (k, v) -> prefs[stringPreferencesKey(k)] = v }
        }
    }
}
