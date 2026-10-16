package com.niagaraclone.launcher.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCustomizationSheet(
    app: AppModel,
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCategorySheet: () -> Unit,
    onOpenEditFolderSheet: () -> Unit
) {
    val context = LocalContext.current
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val favorites by viewModel.favoritePackages.collectAsState()
    val hiddenPackages by viewModel.hiddenPackages.collectAsState()
    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val accentColor = Color(accentColorArgb)

    val isFavorite = favorites.contains(app.packageName)
    val isHidden = hiddenPackages.contains(app.packageName)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141414),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                app.icon?.let { iconDrawable ->
                    Image(
                        bitmap = iconDrawable.toBitmap(width = 48, height = 48).asImageBitmap(),
                        contentDescription = app.label,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Text(
                    text = app.label,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            SheetActionRow(
                icon = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                title = if (isFavorite) "Remove from favorites" else "Edit favorites",
                onClick = {
                    viewModel.toggleFavorite(app.packageName)
                    onDismiss()
                }
            )

            SheetActionRow(
                icon = Icons.Outlined.Info,
                title = "App info",
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", app.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    onDismiss()
                }
            )

            SheetActionRow(
                icon = Icons.Outlined.HourglassEmpty,
                title = "Screen time",
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    onDismiss()
                }
            )

            SheetActionRow(
                icon = Icons.Outlined.AccountTree,
                title = "Add to category",
                onClick = { onOpenCategorySheet() }
            )

            SheetActionRow(
                icon = Icons.Outlined.Delete,
                title = "Uninstall",
                onClick = {
                    val intent = Intent(Intent.ACTION_DELETE).apply {
                        data = Uri.fromParts("package", app.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    onDismiss()
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Advanced",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            AnimatedVisibility(visible = isAdvancedExpanded) {
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    SheetActionRow(
                        icon = Icons.Outlined.OpenInNew,
                        title = "Edit ${app.label} pop-up",
                        onClick = { onOpenEditFolderSheet() }
                    )

                    SheetActionRow(
                        icon = Icons.Outlined.Edit,
                        title = "Edit icon/name",
                        onClick = { showRenameDialog = true }
                    )

                    SheetActionRow(
                        icon = if (isHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        title = if (isHidden) "Unhide from app list" else "Hide from app list",
                        onClick = {
                            viewModel.toggleHidePackage(app.packageName)
                            onDismiss()
                        }
                    )
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            SheetActionRow(
                icon = Icons.Outlined.Settings,
                title = "Cascadia settings",
                onClick = {
                    onDismiss()
                    onOpenSettings()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // --- EDIT NAME DIALOG ---
    if (showRenameDialog) {
        var newName by remember { mutableStateOf(app.label) }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Edit App Name", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.DarkGray
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setCustomAppLabel(app.packageName, newName)
                    showRenameDialog = false
                    onDismiss()
                }) {
                    Text("Save", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            color = Color.White,
            fontWeight = FontWeight.Normal
        )
    }
}
