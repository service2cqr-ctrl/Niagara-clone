package com.niagaraclone.launcher.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HideAppsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val apps by viewModel.apps.collectAsState()
    val hiddenPackages by viewModel.hiddenPackages.collectAsState()
    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val accentColor = Color(accentColorArgb)

    var searchQuery by remember { mutableStateOf("") }

    // Ensure system Back swipe returns to Productivity Settings instead of Main Settings
    BackHandler(onBack = onBack)

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) {
            apps
        } else {
            apps.filter { it.label.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    val (currentlyHiddenApps, visibleApps) = remember(filteredApps, hiddenPackages) {
        filteredApps.partition { hiddenPackages.contains(it.packageName) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hidden apps",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (hiddenPackages.isEmpty()) {
                            "No apps hidden"
                        } else {
                            "${hiddenPackages.size} hidden • Tap icon to launch, long-press for info"
                        },
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                placeholder = { Text("Search apps to hide or launch...", color = Color.Gray, fontSize = 14.sp) },
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
                    focusedContainerColor = Color(0xFF141414),
                    unfocusedContainerColor = Color(0xFF141414)
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                if (currentlyHiddenApps.isNotEmpty()) {
                    item(key = "header_hidden") {
                        Text(
                            text = "HIDDEN APPS (${currentlyHiddenApps.size})",
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp, start = 8.dp)
                        )
                    }

                    items(
                        items = currentlyHiddenApps,
                        key = { "hidden_${it.packageName}" }
                    ) { app ->
                        HideAppRow(
                            app = app,
                            isHidden = true,
                            accentColor = accentColor,
                            onToggleHide = { viewModel.toggleHidePackage(app.packageName) },
                            onLaunchApp = {
                                context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                                    context.startActivity(it)
                                }
                            },
                            onOpenAppInfo = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", app.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }

                    item(key = "divider_hidden") {
                        HorizontalDivider(
                            color = Color(0xFF222222),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }

                item(key = "header_all_apps") {
                    Text(
                        text = "ALL APPS (${visibleApps.size})",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp, start = 8.dp)
                    )
                }

                items(
                    items = visibleApps,
                    key = { "visible_${it.packageName}" }
                ) { app ->
                    HideAppRow(
                        app = app,
                        isHidden = false,
                        accentColor = accentColor,
                        onToggleHide = { viewModel.toggleHidePackage(app.packageName) },
                        onLaunchApp = {
                            context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                                context.startActivity(it)
                            }
                        },
                        onOpenAppInfo = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HideAppRow(
    app: AppModel,
    isHidden: Boolean,
    accentColor: Color,
    onToggleHide: () -> Unit,
    onLaunchApp: () -> Unit,
    onOpenAppInfo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onToggleHide,
                onLongClick = onOpenAppInfo
            )
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        app.icon?.let { iconDrawable ->
            Image(
                bitmap = iconDrawable.toBitmap(width = 44, height = 44).asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        } ?: Spacer(modifier = Modifier.width(50.dp))

        Text(
            text = app.label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        if (isHidden) {
            IconButton(
                onClick = onLaunchApp,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Launch,
                    contentDescription = "Launch hidden app",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onOpenAppInfo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "App info",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Checkbox(
            checked = isHidden,
            onCheckedChange = { onToggleHide() },
            colors = CheckboxDefaults.colors(
                checkedColor = accentColor,
                checkmarkColor = Color.Black,
                uncheckedColor = Color.Gray
            )
        )
    }
}
