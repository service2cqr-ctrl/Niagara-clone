package com.niagaraclone.launcher.backup

import android.content.Context
import android.net.Uri
import com.niagaraclone.launcher.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupManager(
    private val context: Context,
    private val repository: SettingsRepository
) {

    fun snapshot(): String {
        return try {
            repository.exportBackupJson()
        } catch (e: Exception) {
            e.printStackTrace()
            "{}"
        }
    }

    fun restore(jsonStr: String): Boolean {
        return try {
            repository.importBackupJson(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = repository.exportBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            } ?: return@withContext false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                reader.readText()
            } ?: return@withContext false

            repository.importBackupJson(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
