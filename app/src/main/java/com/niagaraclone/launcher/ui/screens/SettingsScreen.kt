package com.niagaraclone.launcher.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.niagaraclone.launcher.data.AppTheme
import com.niagaraclone.launcher.data.SettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: SettingsRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val currentTheme by repository.theme.collectAsState(initial = AppTheme.SYSTEM)
    val currentAccent by repository.accentColor.collectAsState(initial = 0xFF3B82F6.toInt())
    val currentIconPack by repository.iconPackPackage.collectAsState(initial = null)

    var iconPackInput by remember(currentIconPack) { mutableStateOf(currentIconPack ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Niagara Settings") },
                navigationIcon = {
                    Text(
                        text = "← Back",
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                AppTheme.values().forEach { theme ->
                    Button(
                        onClick = {
                            scope.launch { repository.setTheme(theme) }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(theme.name)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Accent Color", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                val colors = listOf(
                    Color(0xFF3B82F6), // Niagara Blue
                    Color(0xFF10B981), // Emerald
                    Color(0xFFEF4444), // Crimson
                    Color(0xFF8B5CF6)  // Purple
                )
                colors.forEach { color ->
                    Button(
                        onClick = {
                            scope.launch {
                                repository.setAccentColor(color.toArgb())
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("●", color = color)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Icon Pack Package", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = iconPackInput,
                onValueChange = { iconPackInput = it },
                label = { Text("Package Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            repository.setIconPack(iconPackInput.ifBlank { null })
                        }
                    }
                ) {
                    Text("Save Icon Pack")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        iconPackInput = ""
                        scope.launch {
                            repository.setIconPack(null)
                        }
                    }
                ) {
                    Text("Reset")
                }
            }
        }
    }
}
