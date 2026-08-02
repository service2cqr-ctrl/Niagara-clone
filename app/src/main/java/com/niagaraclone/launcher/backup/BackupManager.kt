package com.niagaraclone.launcher.backup

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import com.niagaraclone.launcher.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter

/**
 * Exports the launcher's settings (theme, accent color, favorites, hidden apps,
 * icon pack, gesture bindings) to a single JSON file the user can save, and
 * restores from that same format later — this is the whole "backup" feature.
 */
class BackupManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    companion object {
        const val BACKUP_VERSION = 1
    }

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val snapshot = settingsRepository.snapshot()
        val json = JSONObject()
        json.put("backupVersion", BACKUP_VERSION)
        val prefsJson = JSONObject()
        snapshot.forEach { (key: Preferences.Key<*>, value) ->
            prefsJson.put(key.name, value.toString())
        }
        json.put("preferences", prefsJson)
        json.toString(2)
    }

    suspend fun exportToUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = exportToJson()
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream).use { it.write(json) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun importFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext false
            importFromJson(text)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun importFromJson(text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(text)
            val prefsJson = json.getJSONObject("preferences")
            val values = mutableMapOf<String, String>()
            prefsJson.keys().forEach { key -> values[key] = prefsJson.getString(key) }
            settingsRepository.restore(values)
            true
        } catch (e: Exception) {
            false
        }
    }
}
