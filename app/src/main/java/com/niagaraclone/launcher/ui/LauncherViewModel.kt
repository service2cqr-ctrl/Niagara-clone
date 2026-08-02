package com.niagaraclone.launcher.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.data.AppTheme
import com.niagaraclone.launcher.data.SettingsRepository
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

    private val _apps = MutableStateFlow<List<AppModel>>(emptyList())
    val apps: StateFlow<List<AppModel>> = _apps.asStateFlow()

    // Expose DataStore settings directly to Compose UI
    val theme: StateFlow<AppTheme> = repository.theme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    val accentColorArgb: StateFlow<Int> = repository.accentColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0xFF3B82F6.toInt()
        )

    val favoritePackages: StateFlow<Set<String>> = repository.favorites
        .map { it.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    init {
        refreshApps()
    }

    /**
     * Queries PackageManager on a background thread for all launchable apps.
     */
    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }

            val installedApps = resolveInfos.mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                // Skip launcher's own package from the app list
                if (packageName == context.packageName) return@mapNotNull null

                val label = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                AppModel(
                    packageName = packageName,
                    label = label,
                    icon = icon
                )
            }.sortedBy { it.label.lowercase() }

            withContext(Dispatchers.Main) {
                _apps.value = installedApps
            }
        }
    }

    /**
     * Toggles an app in or out of the favorites list.
     */
    fun toggleFavorite(packageName: String) {
        viewModelScope.launch {
            val currentFavs = favoritePackages.value.toMutableSet()
            if (currentFavs.contains(packageName)) {
                currentFavs.remove(packageName)
            } else {
                currentFavs.add(packageName)
            }
            repository.setFavorites(currentFavs.toList())
        }
    }

    /**
     * ViewModel Factory required for MainActivity viewModels delegate
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LauncherViewModel::class.java)) {
                val repository = SettingsRepository(context.applicationContext)
                return LauncherViewModel(repository, context.applicationContext) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
