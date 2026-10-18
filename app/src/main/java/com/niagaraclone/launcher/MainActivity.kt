package com.niagaraclone.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.niagaraclone.launcher.gestures.GestureHost
import com.niagaraclone.launcher.gestures.SwipeDirection
import com.niagaraclone.launcher.services.AudioConnectionReceiver
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.components.WidgetPickerSheet
import com.niagaraclone.launcher.ui.screens.HomeScreen
import com.niagaraclone.launcher.ui.screens.SettingsScreen
import com.niagaraclone.launcher.ui.screens.SmartSearchOverlay
import com.niagaraclone.launcher.ui.screens.WidgetsScreen
import com.niagaraclone.launcher.ui.theme.AppTheme
import com.niagaraclone.launcher.ui.theme.NiagaraCloneTheme

private enum class Page { HOME, WIDGETS, SETTINGS, SEARCH }

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost
    private val audioReceiver = AudioConnectionReceiver()

    private var pendingWidgetId: Int = -1
    private var pendingProviderInfo: AppWidgetProviderInfo? = null

    companion object {
        const val APPWIDGET_HOST_ID = 1024
        private const val REQUEST_CODE_PERMISSIONS = 101
        private const val REQUEST_CODE_CONFIGURE_WIDGET = 127
    }

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkDefaultLauncherStatus()
    }

    // Handles the system "Allow Cascadia Launcher to create widgets?" dialog
    private val bindWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val widgetId = pendingWidgetId
        val providerInfo = pendingProviderInfo
        if (result.resultCode == Activity.RESULT_OK && widgetId != -1 && providerInfo != null) {
            launchWidgetConfigureOrSave(widgetId, providerInfo)
        } else {
            if (widgetId != -1) {
                appWidgetHost.deleteAppWidgetId(widgetId)
            }
            pendingWidgetId = -1
            pendingProviderInfo = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }

        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, APPWIDGET_HOST_ID)

        val audioFilter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        registerReceiver(audioReceiver, audioFilter)
        AudioConnectionReceiver.checkAudioConnectionState(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)

        setContent {
            val currentAppTheme by viewModel.theme.collectAsState()
            val accentArgb by viewModel.accentColorArgb.collectAsState()
            var page by remember { mutableStateOf(Page.HOME) }
            var triggerWidgetMoveEdit by remember { mutableStateOf(false) }
            var showCustomWidgetPicker by remember { mutableStateOf(false) }

            LaunchedEffect(currentAppTheme, page) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        val blurRadius = when {
                            currentAppTheme == AppTheme.GLASS && page != Page.HOME -> 65
                            currentAppTheme == AppTheme.GLASS && page == Page.HOME -> 18
                            else -> 0
                        }
                        window.setBackgroundBlurRadius(blurRadius)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            NiagaraCloneTheme(
                appTheme = currentAppTheme,
                accentColor = Color(accentArgb)
            ) {
                GestureHost(
                    modifier = Modifier.fillMaxSize(),
                    onTriggerAction = { direction ->
                        when (direction) {
                            SwipeDirection.UP -> page = Page.SEARCH
                            SwipeDirection.DOWN -> {
                                if (page == Page.HOME) openNotificationShade(this@MainActivity)
                            }
                        }
                    }
                ) { _ ->
                    AnimatedContent(
                        targetState = page,
                        label = "page_transition",
                        transitionSpec = { fadeIn() togetherWith fadeOut() }
                    ) { current ->
                        when (current) {
                            Page.HOME -> HomeScreen(
                                viewModel = viewModel,
                                appWidgetHost = appWidgetHost,
                                appWidgetManager = appWidgetManager,
                                isEditingWidgetExternal = triggerWidgetMoveEdit,
                                onAddWidgetClick = { showCustomWidgetPicker = true },
                                onOpenSettings = {
                                    triggerWidgetMoveEdit = false
                                    page = Page.SETTINGS
                                },
                                onRequestSetDefaultLauncher = { requestDefaultLauncher() },
                                onOpenSearch = { page = Page.SEARCH }
                            )
                            Page.WIDGETS -> WidgetsScreen(
                                viewModel = viewModel,
                                appWidgetHost = appWidgetHost,
                                appWidgetManager = appWidgetManager,
                                onAddWidgetClick = { showCustomWidgetPicker = true },
                                onDismiss = { page = Page.HOME }
                            )
                            Page.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                onDismiss = { page = Page.HOME },
                                onAddWidgetClick = {
                                    page = Page.HOME
                                    showCustomWidgetPicker = true
                                },
                                onMoveWidgetClick = {
                                    triggerWidgetMoveEdit = true
                                    page = Page.HOME
                                }
                            )
                            Page.SEARCH -> SmartSearchOverlay(
                                viewModel = viewModel,
                                onDismiss = { page = Page.HOME }
                            )
                        }
                    }

                    if (showCustomWidgetPicker) {
                        WidgetPickerSheet(
                            appWidgetManager = appWidgetManager,
                            accentColor = Color(accentArgb),
                            onWidgetSelected = { providerInfo ->
                                showCustomWidgetPicker = false
                                bindAndConfigureWidget(providerInfo)
                            },
                            onDismiss = { showCustomWidgetPicker = false }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            appWidgetHost.startListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        AudioConnectionReceiver.checkAudioConnectionState(this)
    }

    override fun onStop() {
        super.onStop()
        try {
            appWidgetHost.stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(audioReceiver) } catch (e: Exception) { }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadInstalledApps()
        viewModel.checkDefaultLauncherStatus()
        viewModel.refreshPermissions()
        AudioConnectionReceiver.checkAudioConnectionState(this)
    }

    fun requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                roleLauncher.launch(intent)
            }
        } else {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            startActivity(intent)
        }
    }

    private fun bindAndConfigureWidget(providerInfo: AppWidgetProviderInfo) {
        val widgetId = appWidgetHost.allocateAppWidgetId()
        pendingWidgetId = widgetId
        pendingProviderInfo = providerInfo

        val isBound = try {
            appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, providerInfo.provider)
        } catch (e: Exception) {
            false
        }

        if (isBound) {
            launchWidgetConfigureOrSave(widgetId, providerInfo)
        } else {
            // Request user permission to bind the widget to the selected provider
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
            }
            bindWidgetLauncher.launch(bindIntent)
        }
    }

    private fun launchWidgetConfigureOrSave(widgetId: Int, providerInfo: AppWidgetProviderInfo) {
        if (providerInfo.configure != null) {
            try {
                // Must use AppWidgetHost.startAppWidgetConfigureActivityForResult on Android 11+ to avoid SecurityException!
                appWidgetHost.startAppWidgetConfigureActivityForResult(
                    this,
                    widgetId,
                    0,
                    REQUEST_CODE_CONFIGURE_WIDGET,
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
                // Some widgets declare a configure activity that isn't exported; save the widget directly as fallback
                viewModel.addWidgetId(widgetId)
                Toast.makeText(this, "Widget added to home screen", Toast.LENGTH_SHORT).show()
                pendingWidgetId = -1
                pendingProviderInfo = null
            }
        } else {
            viewModel.addWidgetId(widgetId)
            Toast.makeText(this, "Widget added to home screen", Toast.LENGTH_SHORT).show()
            pendingWidgetId = -1
            pendingProviderInfo = null
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CONFIGURE_WIDGET) {
            val widgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId) ?: pendingWidgetId
            if (resultCode == Activity.RESULT_OK && widgetId != -1) {
                viewModel.addWidgetId(widgetId)
                Toast.makeText(this, "Widget added to home screen", Toast.LENGTH_SHORT).show()
            } else if (widgetId != -1) {
                appWidgetHost.deleteAppWidgetId(widgetId)
            }
            pendingWidgetId = -1
            pendingProviderInfo = null
        }
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
    } catch (e: Exception) { }
}
