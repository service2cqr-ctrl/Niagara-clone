package com.niagaraclone.launcher.ui.components

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPopUpFolderSheet(
    parentApp: AppModel,
    viewModel: LauncherViewModel,
    allApps: List<AppModel>,
    onDismiss: () -> Unit
) {
    val existingSubApps = remember(parentApp) {
        viewModel.getFolderSubApps(parentApp.packageName).map { it.packageName }.toSet()
    }
    val selectedPackages = remember { mutableStateOf(existingSubApps) }
    var searchQuery by remember { mutableStateOf("") }

    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val accentColor = Color(accentColorArgb)

    val candidateApps = remember(allApps, parentApp, searchQuery) {
        allApps.filter { it.packageName != parentApp.packageName }.let { list ->
            if (searchQuery.isBlank()) list
            else list.filter { it.label.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141414),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Edit ${parentApp.label} pop-up",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${selectedPackages.value.size} apps selected",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                TextButton(
                    onClick = {
                        viewModel.setFolderSubApps(parentApp.packageName, selectedPackages.value)
                        onDismiss()
                    }
                ) {
                    Text("Save", color = accentColor, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Filter Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                placeholder = { Text("Search apps to add...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF2C2C2C),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1A1A1A),
                    unfocusedContainerColor = Color(0xFF1A1A1A)
                )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(
                    items = candidateApps,
                    key = { it.packageName }
                ) { app ->
                    val isChecked = selectedPackages.value.contains(app.packageName)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val updated = selectedPackages.value.toMutableSet()
                                if (isChecked) updated.remove(app.packageName) else updated.add(app.packageName)
                                selectedPackages.value = updated
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        app.icon?.let { icon ->
                            Image(
                                bitmap = icon.toBitmap(width = 40, height = 40).asImageBitmap(),
                                contentDescription = app.label,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        } ?: Spacer(modifier = Modifier.width(46.dp))

                        Text(
                            text = app.label,
                            fontSize = 15.sp,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                val updated = selectedPackages.value.toMutableSet()
                                if (checked) updated.add(app.packageName) else updated.remove(app.packageName)
                                selectedPackages.value = updated
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = accentColor,
                                checkmarkColor = Color.Black,
                                uncheckedColor = Color.Gray
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
