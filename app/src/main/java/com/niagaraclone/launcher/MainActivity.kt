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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import com.niagaraclone.launcher.gestures.GestureHost
import com.niagaraclone.launcher.gestures.SwipeDirection
import com.niagaraclone.launcher.services.AudioConnectionReceiver
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.screens.HomeScreen
import com.niagaraclone.launcher.ui.screens.SettingsScreen
import com.niagaraclone.launcher.ui.screens.SmartSearchOverlay
import com.niagaraclone.launcher.ui.screens.WidgetsScreen
import com.niagaraclone.launcher.ui.theme.AppTheme
import com.niagaraclone.launcher.ui.theme.LocalLauncherPalette
import com.niagaraclone.launcher.ui.theme.NiagaraCloneTheme
import com.niagaraclone.launcher.ui.theme.glassSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            var showWidgetPickerSheet by remember { mutableStateOf(false) }

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
                                onAddWidgetClick = { showWidgetPickerSheet = true },
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
                                onAddWidgetClick = { showWidgetPickerSheet = true },
                                onDismiss = { page = Page.HOME }
                            )
                            Page.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                onDismiss = { page = Page.HOME },
                                onAddWidgetClick = {
                                    page = Page.HOME
                                    showWidgetPickerSheet = true
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

                    if (showWidgetPickerSheet) {
                        InstalledWidgetPickerSheet(
                            appWidgetManager = appWidgetManager,
                            accentColor = Color(accentArgb),
                            onWidgetSelected = { providerInfo ->
                                showWidgetPickerSheet = false
                                bindAndConfigureWidget(providerInfo)
                            },
                            onDismiss = { showWidgetPickerSheet = false }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try { appWidgetHost.startListening() } catch (e: Exception) { e.printStackTrace() }
        AudioConnectionReceiver.checkAudioConnectionState(this)
    }

    override fun onStop() {
        super.onStop()
        try { appWidgetHost.stopListening() } catch (e: Exception) { e.printStackTrace() }
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
                appWidgetHost.startAppWidgetConfigureActivityForResult(
                    this,
                    widgetId,
                    0,
                    REQUEST_CODE_CONFIGURE_WIDGET,
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
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

private data class InstalledWidgetItem(
    val appLabel: String,
    val widgetLabel: String,
    val providerInfo: AppWidgetProviderInfo,
    val previewDrawable: android.graphics.drawable.Drawable?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstalledWidgetPickerSheet(
    appWidgetManager: AppWidgetManager,
    accentColor: Color,
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalLauncherPalette.current
    var searchQuery by remember { mutableStateOf("") }
    var allWidgets by remember { mutableStateOf<List<InstalledWidgetItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val providers = appWidgetManager.installedProviders
            val mapped = providers.mapNotNull { info ->
                try {
                    val pkgName = info.provider.packageName
                    val appInfo = pm.getApplicationInfo(pkgName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val widgetTitle = info.loadLabel(pm) ?: appName
                    val icon = info.loadPreviewImage(context, 0)
                        ?: info.loadIcon(context, 0)
                        ?: pm.getApplicationIcon(pkgName)

                    InstalledWidgetItem(
                        appLabel = appName,
                        widgetLabel = widgetTitle,
                        providerInfo = info,
                        previewDrawable = icon
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedWith(compareBy({ it.appLabel.lowercase() }, { it.widgetLabel.lowercase() }))

            allWidgets = mapped
            isLoading = false
        }
    }

    val filteredWidgets = remember(allWidgets, searchQuery) {
        if (searchQuery.isBlank()) {
            allWidgets
        } else {
            val q = searchQuery.trim()
            allWidgets.filter {
                it.appLabel.contains(q, ignoreCase = true) ||
                        it.widgetLabel.contains(q, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.sheetBackground,
        contentColor = palette.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Widgets,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Select a widget",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search widgets or apps...", color = palette.textSecondary, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accentColor) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = palette.textSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = palette.borderColor,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    focusedContainerColor = palette.cardBackground,
                    unfocusedContainerColor = palette.cardBackground
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (filteredWidgets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No widgets found", color = palette.textSecondary, fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = filteredWidgets,
                        key = { "${it.providerInfo.provider.packageName}/${it.providerInfo.provider.className}" }
                    ) { item ->
                        val rowShape = RoundedCornerShape(16.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassSurface(shape = rowShape, isSheet = false, borderWidth = 1.dp)
                                .clickable { onWidgetSelected(item.providerInfo) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item.previewDrawable?.let { drawable ->
                                Image(
                                    bitmap = drawable.toBitmap(width = 72, height = 72).asImageBitmap(),
                                    contentDescription = item.widgetLabel,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.widgetLabel,
                                    color = palette.textPrimary,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.appLabel} • ${item.providerInfo.minWidth}×${item.providerInfo.minHeight} dp",
                                    color = palette.textSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
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
