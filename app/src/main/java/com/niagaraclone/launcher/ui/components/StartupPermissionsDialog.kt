package com.niagaraclone.launcher.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.niagaraclone.launcher.ui.LauncherViewModel

@Composable
fun StartupPermissionsDialog(
    viewModel: LauncherViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var showNotificationAccessPrompt by remember { mutableStateOf(false) }

    val requiredRuntimePermissions = remember {
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.READ_CONTACTS
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.loadCalendarEvents()
        viewModel.fetchLiveWeatherForCurrentMode()

        if (!isNotificationListenerGranted(context)) {
            showNotificationAccessPrompt = true
        } else {
            onComplete()
        }
    }

    LaunchedEffect(Unit) {
        val missingPermissions = requiredRuntimePermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else if (!isNotificationListenerGranted(context)) {
            showNotificationAccessPrompt = true
        } else {
            onComplete()
        }
    }

    if (showNotificationAccessPrompt) {
        AlertDialog(
            onDismissRequest = {
                showNotificationAccessPrompt = false
                onComplete()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = Color(0xFF4DD0E1))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Notification Access Required", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "To display media player controls, unread badges, and live notification dots on the wave indexer, please allow Notification Access in system settings.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationAccessPrompt = false
                        openNotificationListenerSettings(context)
                        onComplete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DD0E1))
                ) {
                    Text("Grant Access", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotificationAccessPrompt = false
                        onComplete()
                    }
                ) {
                    Text("Skip for now", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}

private fun isNotificationListenerGranted(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(pkgName)
}

private fun openNotificationListenerSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
