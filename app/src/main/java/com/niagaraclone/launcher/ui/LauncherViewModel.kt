package com.niagaraclone.launcher.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niagaraclone.launcher.backup.BackupManager
import com.niagaraclone.launcher.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LauncherViewModel(
    private val appContext: Context,
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val iconPackRepository: IconPackRepository,
    val backupManager: BackupManager
) : ViewModel() {

    private val _allApps = MutableStateFlow<List<AppEntry>>(emptyList())
    val allApps: StateFlow<List<AppEntry>> = _allApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val favorites: StateFlow<List<String>> = settingsRepository.favorites
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())

    val hiddenApps: StateFlow<List<String>> = settingsRepository.hiddenApps
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())

    val theme: StateFlow<AppTheme> = settingsRepository.theme
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, AppTheme.SYSTEM)

    val accentColorArgb: StateFlow<Int> = settingsRepository.accentColor
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, 0xFF3B82F6.toInt())

    val iconPackPackage: StateFlow<String?> = settingsRepository.iconPackPackage
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, null)

    /** Visible apps = all apps minus hidden ones, filtered by the current search query. */
    val visibleApps: StateFlow<List<AppEntry>> =
        combine(_allApps, hiddenApps, _searchQuery) { apps, hidden, query ->
            apps.filter { it.packageName !in hidden }
                .filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())

    init {
        refreshApps()
        viewModelScope.launch {
            iconPackPackage.collect { pkg -> iconPackRepository.selectIconPack(pkg) }
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            _allApps.value = appRepository.getLaunchableApps()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun launchApp(entry: AppEntry) = appRepository.launch(entry)
    fun openAppInfo(entry: AppEntry) = appRepository.openAppInfo(entry)
    fun uninstallApp(entry: AppEntry) = appRepository.uninstall(entry)
    fun themedIconFor(entry: AppEntry) = iconPackRepository.getThemedIcon(entry)
    fun defaultIconFor(entry: AppEntry) = appRepository.loadDefaultIcon(entry)

    fun toggleFavorite(entry: AppEntry) {
        viewModelScope.launch {
            val current = favorites.value
            val updated = if (entry.packageName in current) {
                current - entry.packageName
            } else {
                (current + entry.packageName).take(6) // Niagara caps favorites to keep the row compact
            }
            settingsRepository.setFavorites(updated)
        }
    }

    fun setAppHidden(entry: AppEntry, hidden: Boolean) {
        viewModelScope.launch {
            val current = hiddenApps.value
            val updated = if (hidden) current + entry.packageName else current - entry.packageName
            settingsRepository.setHiddenApps(updated)
        }
    }

    fun setTheme(theme: AppTheme) = viewModelScope.launch { settingsRepository.setTheme(theme) }
    fun setAccentColor(argb: Int) = viewModelScope.launch { settingsRepository.setAccentColor(argb) }
    fun setIconPack(pkg: String?) = viewModelScope.launch { settingsRepository.setIconPack(pkg) }

    suspend fun getAvailableIconPacks() = iconPackRepository.getAvailableIconPacks()

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appContext = context.applicationContext
            val settingsRepository = SettingsRepository(appContext)
            @Suppress("UNCHECKED_CAST")
            return LauncherViewModel(
                appContext = appContext,
                appRepository = AppRepository(appContext),
                settingsRepository = settingsRepository,
                iconPackRepository = IconPackRepository(appContext),
                backupManager = BackupManager(appContext, settingsRepository)
            ) as T
        }
    }
}
