package com.niagaraclone.launcher.backup

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import com.niagaraclone.launcher.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class BackupManager(
    private val context: Context,
    private val repository: SettingsRepository
) {

    suspend fun createBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val snapshotMap: Map<Preferences.Key<*>, Any> = repository.snapshot()
            val jsonObject = JSONObject()

            for ((key, value) in snapshotMap) {
                jsonObject.put(key.name, value.toString())
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonObject.toString(4).toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                it.readText()
            } ?: return@withContext false

            val jsonObject = JSONObject(jsonString)
            val restoreMap = mutableMapOf<String, String>()

            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                restoreMap[key] = jsonObject.getString(key)
            }

            repository.restore(restoreMap)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
