package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.theme.LocalLauncherPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToCategorySheet(
    app: AppModel,
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit
) {
    val palette = LocalLauncherPalette.current
    val selectedMusicPackages by viewModel.selectedMusicPackages.collectAsState()
    val hiddenPackages by viewModel.hiddenPackages.collectAsState()
    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val accentColor = Color(accentColorArgb)

    val isMusicApp = selectedMusicPackages.contains(app.packageName)
    val isHiddenApp = hiddenPackages.contains(app.packageName)

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.sheetBackground,
        contentColor = palette.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Add to category",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            CategoryRow(
                icon = Icons.Outlined.Headphones,
                title = "Music Apps",
                subtitle = if (isMusicApp) "Added to music player triggers • Tap to remove" else "Appear when headphones or speakers are connected",
                isChecked = isMusicApp,
                accentColor = accentColor,
                palette = palette,
                onClick = {
                    viewModel.toggleMusicPackage(app.packageName)
                    onDismiss()
                }
            )

            CategoryRow(
                icon = Icons.Outlined.VisibilityOff,
                title = "Hidden apps",
                subtitle = if (isHiddenApp) "Currently hidden from app list • Tap to unhide" else "Tidy up your app list to find apps quicker",
                isChecked = isHiddenApp,
                accentColor = accentColor,
                palette = palette,
                onClick = {
                    viewModel.toggleHidePackage(app.packageName)
                    onDismiss()
                }
            )

            CategoryRow(
                icon = Icons.Outlined.CreateNewFolder,
                title = "Create pop-up folder",
                subtitle = "Group ${app.label} into a new custom folder",
                isChecked = false,
                accentColor = accentColor,
                palette = palette,
                onClick = { showCreateFolderDialog = true }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            containerColor = palette.sheetBackground,
            title = { Text("Create Pop-up Folder", color = palette.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a name for the new folder containing ${app.label}:", color = palette.textSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        placeholder = { Text("Folder name", color = palette.textSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = palette.borderColor,
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary,
                            focusedContainerColor = palette.cardBackground,
                            unfocusedContainerColor = palette.cardBackground
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            val currentSubApps = viewModel.repository.getFolderSubAppPackages(newFolderName)
                            val updatedSubApps = currentSubApps + app.packageName
                            viewModel.setFolderSubApps(newFolderName, updatedSubApps)
                            showCreateFolderDialog = false
                            onDismiss()
                        }
                    }
                ) {
                    Text("Create", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun CategoryRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    isChecked: Boolean,
    accentColor: Color,
    palette: com.niagaraclone.launcher.ui.theme.LauncherPalette,
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
            tint = if (isChecked) accentColor else palette.textPrimary.copy(alpha = 0.9f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = if (isChecked) accentColor else palette.textPrimary,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 12.sp, color = palette.textSecondary)
            }
        }
        if (isChecked) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
