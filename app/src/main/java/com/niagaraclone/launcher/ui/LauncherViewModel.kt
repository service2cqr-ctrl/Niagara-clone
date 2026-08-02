package com.niagaraclone.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.data.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val appRepository = AppRepository(application)

    val installedApps: StateFlow<List<AppModel>> = appRepository.installedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritePackages: StateFlow<Set<String>> = appRepository.favoritePackages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        viewModelScope.launch {
            appRepository.loadInstalledApps()
        }
    }

    fun toggleFavorite(packageName: String) {
        appRepository.toggleFavorite(packageName)
    }
}
