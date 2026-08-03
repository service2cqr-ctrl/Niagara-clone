package com.niagaraclone.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.niagaraclone.launcher.gestures.GestureHost
import com.niagaraclone.launcher.gestures.SwipeDirection
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.screens.HomeScreen
import com.niagaraclone.launcher.ui.screens.SettingsScreen
import com.niagaraclone.launcher.ui.screens.WidgetsScreen
import com.niagaraclone.launcher.ui.theme.NiagaraCloneTheme

private enum class Page { HOME, WIDGETS, SETTINGS }

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModel.Factory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Ensures wallpaper extends behind system status bar and gesture navigation bar
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. Tells Android WindowManager to render system wallpaper directly behind this activity window
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)

        setContent {
            val theme by viewModel.theme.collectAsState()
            val accentArgb by viewModel.accentColorArgb.collectAsState()

            NiagaraCloneTheme(appTheme = theme, accentColor = Color(accentArgb)) {
                var page by remember { mutableStateOf(Page.HOME) }

                GestureHost(
                    modifier = Modifier.fillMaxSize(),
                    onSwipe = { direction ->
                        page = when (direction) {
                            SwipeDirection.DOWN -> {
                                if (page == Page.HOME) {
                                    openNotificationShade(this@MainActivity)
                                }
                                page
                            }
                            SwipeDirection.LEFT -> if (page == Page.HOME) Page.WIDGETS else page
                            SwipeDirection.RIGHT -> if (page == Page.WIDGETS) Page.HOME else page
                            SwipeDirection.UP -> page
                        }
                    },
                    onDoubleTap = { /* Extension point for double-tap gestures */ }
                ) {
                    AnimatedContent(
                        targetState = page,
                        label = "page_transition",
                        transitionSpec = { fadeIn() togetherWith fadeOut() }
                    ) { current ->
                        when (current) {
                            Page.HOME -> HomeScreen(
                                viewModel = viewModel,
                                onOpenSettings = { page = Page.SETTINGS }
                            )
                            Page.WIDGETS -> WidgetsScreen()
                            Page.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                onDismiss = { page = Page.HOME }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshApps()
    }
}

@SuppressLint("WrongConstant", "PrivateApi")
private fun openNotificationShade(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
        val methodName = if (Build.VERSION.SDK_INT >= 17) "expandNotificationsPanel" else "expand"
        val method = statusBarManagerClass.getMethod(methodName)
        method.invoke(statusBarService)
    } catch (e: Exception) {
        // Best-effort attempt across OEM devices; ignore failure silently
    }
}
