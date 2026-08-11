package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
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
                Text(
                    text = "Edit ${parentApp.label} Pop-up",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                TextButton(
                    onClick = {
                        viewModel.setFolderSubApps(parentApp.packageName, selectedPackages.value)
                        onDismiss()
                    }
                ) {
                    Text("Save", color = Color(0xFF4DD0E1), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(allApps.filter { it.packageName != parentApp.packageName }) { app ->
                    val isChecked = selectedPackages.value.contains(app.packageName)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val updated = selectedPackages.value.toMutableSet()
                                if (isChecked) updated.remove(app.packageName) else updated.add(app.packageName)
                                selectedPackages.value = updated
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        app.icon?.let { icon ->
                            Image(
                                bitmap = icon.toBitmap(width = 36, height = 36).asImageBitmap(),
                                contentDescription = app.label,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
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
                                if (checked == true) updated.add(app.packageName) else updated.remove(app.packageName)
                                selectedPackages.value = updated
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4DD0E1))
                        )
                    }
                }
            }
        }
    }
}
