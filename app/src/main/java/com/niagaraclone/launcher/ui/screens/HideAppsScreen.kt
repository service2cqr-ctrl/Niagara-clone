package com.niagaraclone.launcher.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.niagaraclone.launcher.ui.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HideAppsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val apps by viewModel.apps.collectAsState()
    val hiddenPackages by viewModel.hiddenPackages.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Hide Apps", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Text(
                text = "Selected apps will be hidden from the app drawer list.",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(apps) { app ->
                    val isHidden = hiddenPackages.contains(app.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleHidePackage(app.packageName) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        app.icon?.let { iconDrawable ->
                            Image(
                                bitmap = iconDrawable.toBitmap(width = 40, height = 40).asImageBitmap(),
                                contentDescription = app.label,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Text(
                            text = app.label,
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = isHidden,
                            onCheckedChange = { viewModel.toggleHidePackage(app.packageName) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF4DD0E1),
                                uncheckedColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }
    }
}
