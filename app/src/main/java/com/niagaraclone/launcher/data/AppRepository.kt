package com.niagaraclone.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AppRepository(context: Context) {
    private val packageManager: PackageManager = context.packageManager

    private val _installedApps = MutableStateFlow<List<AppModel>>(emptyList())
    val installedApps: StateFlow<List<AppModel>> = _installedApps.asStateFlow()

    private val _favoritePackages = MutableStateFlow<Set<String>>(setOf())
    val favoritePackages: StateFlow<Set<String>> = _favoritePackages.asStateFlow()

    suspend fun loadInstalledApps() = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        val apps = resolveInfos.mapNotNull { resolveInfo ->
            try {
                val pkgName = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(packageManager).toString()
                val icon = resolveInfo.loadIcon(packageManager)
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
