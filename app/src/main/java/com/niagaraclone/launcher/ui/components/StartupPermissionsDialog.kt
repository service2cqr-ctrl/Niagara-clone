package com.niagaraclone.launcher.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.niagaraclone.launcher.ui.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartupPermissionsDialog(
    viewModel: LauncherViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current

    val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCalendar = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    val notificationGranted by viewModel.notificationPermissionGranted.collectAsState()

    var requestLocation by remember { mutableStateOf(!hasLocation) }
    var requestCalendar by remember { mutableStateOf(!hasCalendar) }
    var requestNotification by remember { mutableStateOf(!notificationGranted) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        requestCalendar = false
        if (!requestLocation && !requestCalendar && !requestNotification) onComplete()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        requestLocation = false
        if (!requestLocation && !requestCalendar && !requestNotification) onComplete()
    }

    if (!requestLocation && !requestCalendar && !requestNotification) {
        LaunchedEffect(Unit) { onComplete() }
        return
    }

    ModalBottomSheet(
        onDismissRequest = onComplete,
        containerColor = Color(0xFF181818),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Permissions Request",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enable access to personalize your home screen features.",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (requestNotification) {
                PermissionRequestRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notification Access",
                    description = "Show badges, music player, and inline replies",
                    onGrant = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        requestNotification = false
                    }
                )
            }

            if (requestCalendar) {
                PermissionRequestRow(
                    icon = Icons.Outlined.CalendarToday,
                    title = "Calendar Access",
                    description = "Display upcoming agenda events in header",
                    onGrant = {
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    }
                )
            }

            if (requestLocation) {
                PermissionRequestRow(
                    icon = Icons.Outlined.LocationOn,
                    title = "Location Access",
                    description = "Get accurate live weather updates",
                    onGrant = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onComplete) {
                    Text("Skip for now", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionRequestRow(
    icon: ImageVector,
    title: String,
    description: String,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4DD0E1),
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = description, fontSize = 12.sp, color = Color.Gray)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DD0E1), contentColor = Color.Black),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
