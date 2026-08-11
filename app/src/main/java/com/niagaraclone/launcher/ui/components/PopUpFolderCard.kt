package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel

@Composable
fun PopUpFolderCard(
    viewModel: LauncherViewModel,
    parentPackage: String,
    folderName: String,
    subApps: List<AppModel>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val shortcuts = remember(parentPackage) { viewModel.getAppShortcuts(parentPackage) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E1E1E),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = folderName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }

            // Sub-apps List (if added)
            if (subApps.isNotEmpty()) {
                subApps.forEach { subApp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.packageManager.getLaunchIntentForPackage(subApp.packageName)?.let {
                                    context.startActivity(it)
                                }
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        subApp.icon?.let { icon ->
                            Image(
                                bitmap = icon.toBitmap(width = 36, height = 36).asImageBitmap(),
                                contentDescription = subApp.label,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                        }
                        Text(
                            text = subApp.label,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // App Native Shortcuts (if any)
            if (shortcuts.isNotEmpty()) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                Text(
                    text = "Shortcuts",
                    color = Color(0xFF4DD0E1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                shortcuts.forEach { shortcut ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.launchAppShortcut(shortcut.shortcutInfo)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        shortcut.icon?.let { icon ->
                            Image(
                                bitmap = icon.toBitmap(width = 36, height = 36).asImageBitmap(),
                                contentDescription = shortcut.label,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                        }
                        Text(
                            text = shortcut.label,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
