package com.niagaraclone.launcher.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.niagaraclone.launcher.data.AppTheme
import com.niagaraclone.launcher.data.IconPackInfo
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.theme.AccentPresets
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: LauncherViewModel, onDismiss: () -> Unit) {
    val theme by viewModel.theme.collectAsState()
    val accent by viewModel.accentColorArgb.collectAsState()
    val currentIconPack by viewModel.iconPackPackage.collectAsState()
    val scope = rememberCoroutineScope()

    var iconPacks by remember { mutableStateOf<List<IconPackInfo>>(emptyList()) }
    LaunchedEffect(Unit) { iconPacks = viewModel.getAvailableIconPacks() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { scope.launch { viewModel.backupManager.exportToUri(it) } } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { scope.launch { viewModel.backupManager.importFromUri(it) } } }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.padding(vertical = 8.dp)) {
            AppTheme.values().forEach { option ->
                FilterChip(
                    selected = theme == option,
                    onClick = { viewModel.setTheme(option) },
                    label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Accent color", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.padding(vertical = 8.dp)) {
            AccentPresets.forEach { color ->
                val argb = color.toArgb()
                Box(
                    Modifier
                        .padding(end = 12.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { viewModel.setAccentColor(argb) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Icon pack", style = MaterialTheme.typography.titleMedium)
        Column(Modifier.padding(vertical = 8.dp)) {
            RadioRow(
                label = "Default icons",
                selected = currentIconPack == null,
                onClick = { viewModel.setIconPack(null) }
            )
            iconPacks.forEach { pack ->
                RadioRow(
                    label = pack.label,
                    selected = currentIconPack == pack.packageName,
                    onClick = { viewModel.setIconPack(pack.packageName) }
                )
            }
            if (iconPacks.isEmpty()) {
                Text(
                    "No icon pack apps installed",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Backup", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.padding(vertical = 8.dp)) {
            Button(onClick = { exportLauncher.launch("niagara_clone_backup.json") }) {
                Text("Export settings")
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                Text("Restore")
            }
        }

        Spacer(Modifier.weight(1f))
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Close")
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
    )
}
