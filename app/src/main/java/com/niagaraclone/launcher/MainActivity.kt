package com.niagaraclone.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.niagaraclone.launcher.gestures.GestureHost
import com.niagaraclone.launcher.gestures.SwipeDirection
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.screens.AppDrawerScreen
import com.niagaraclone.launcher.ui.screens.HomeScreen
import com.niagaraclone.launcher.ui.screens.SettingsScreen
import com.niagaraclone.launcher.ui.screens.WidgetsScreen
import com.niagaraclone.launcher.ui.theme.NiagaraCloneTheme

private enum class Page { HOME, APP_DRAWER, WIDGETS, SETTINGS }

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModel.Factory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val theme by viewModel.theme.collectAsState()
            val accentArgb by viewModel.accentColorArgb.collectAsState()

            NiagaraCloneTheme(appTheme = theme, accentColor = Color(accentArgb)) {
                var page by remember { mutableStateOf(Page.HOME) }

                GestureHost(
                    modifier = Modifier.fillMaxSize(),
                    onSwipe = { direction ->
                        page = when (direction) {
                            SwipeDirection.UP -> if (page == Page.HOME) Page.APP_DRAWER else page
                            SwipeDirection.DOWN -> {
                                if (page == Page.HOME) openNotificationShade(this@MainActivity)
                                if (page == Page.APP_DRAWER) Page.HOME else page
                            }
                            SwipeDirection.LEFT -> if (page == Page.HOME) Page.WIDGETS else page
                            SwipeDirection.RIGHT -> if (page == Page.WIDGETS) Page.HOME else page
                        }
                    },
                    onDoubleTap = { /* e.g. lock screen — needs device admin, left as extension point */ }
                ) {
                    AnimatedContent(targetState = page, label = "page") { current ->
                        when (current) {
                            Page.HOME -> HomeScreen(
                                viewModel = viewModel,
                                onOpenAppDrawer = { page = Page.APP_DRAWER }
                            )
                            Page.APP_DRAWER -> AppDrawerScreen(
                                viewModel = viewModel,
                                onDismiss = { page = Page.HOME }
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
        // Reflection into StatusBarManager is best-effort across OEMs/versions; fail silently.
    }
}
