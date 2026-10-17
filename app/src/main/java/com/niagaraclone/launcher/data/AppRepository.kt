package com.niagaraclone.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.niagaraclone.launcher.utils.IconPackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager
    private val iconPackManager = IconPackManager(context)

    private val _installedApps = MutableStateFlow<List<AppModel>>(emptyList())
    val installedApps: StateFlow<List<AppModel>> = _installedApps.asStateFlow()

    private val _favoritePackages = MutableStateFlow<Set<String>>(setOf())
    val favoritePackages: StateFlow<Set<String>> = _favoritePackages.asStateFlow()

    suspend fun loadInstalledApps(selectedIconPack: String = "SYSTEM") = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        
        val customLabels = context.getSharedPreferences("custom_app_labels", Context.MODE_PRIVATE)

        val apps = resolveInfos.mapNotNull { resolveInfo ->
            try {
                val pkgName = resolveInfo.activityInfo.packageName
                val defaultLabel = resolveInfo.loadLabel(packageManager).toString()
                val label = customLabels.getString(pkgName, defaultLabel) ?: defaultLabel

                // Load themed icon if custom icon pack is selected, else system app icon
                var icon = if (selectedIconPack.isNotEmpty() && selectedIconPack != "SYSTEM") {
                    iconPackManager.loadIconFromPack(selectedIconPack, pkgName)
                } else {
                    null
                }

                if (icon == null) {
                    icon = resolveInfo.loadIcon(packageManager)
                }

                AppModel(
                    packageName = pkgName,
                    label = label,
                    icon = icon
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }.sortedBy { it.label.lowercase() }

        _installedApps.value = apps
        
        // Default pin first 4 apps as favorites if none selected
        if (_favoritePackages.value.isEmpty() && apps.isNotEmpty()) {
            _favoritePackages.value = apps.take(4).map { it.packageName }.toSet()
        }
    }

    fun setCustomAppLabel(packageName: String, newLabel: String) {
        if (packageName.isBlank()) return
        val prefs = context.getSharedPreferences("custom_app_labels", Context.MODE_PRIVATE)
        prefs.edit().putString(packageName, newLabel).apply()

        // Update in-memory state flow immediately
        _installedApps.value = _installedApps.value.map { app ->
            if (app.packageName == packageName) app.copy(label = newLabel) else app
        }
    }

    fun toggleFavorite(packageName: String) {
        val currentFavorites = _favoritePackages.value.toMutableSet()
        if (currentFavorites.contains(packageName)) {
            currentFavorites.remove(packageName)
        } else {
            currentFavorites.add(packageName)
        }
        _favoritePackages.value = currentFavorites
    }
}
