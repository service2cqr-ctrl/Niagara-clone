package com.niagaraclone.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSearchOverlay(
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val apps by viewModel.apps.collectAsState()

    // Dynamically filter installed apps based solely on input query
    val filteredApps = remember(query, apps) {
        if (query.isBlank()) {
            emptyList()
        } else {
            apps.filter { it.label.contains(query, ignoreCase = true) }
        }
    }

    // Recent/Last opened app result when search query is empty
    val lastResultApp = remember(apps) {
        apps.firstOrNull { it.label.contains("Cascadia", ignoreCase = true) } ?: apps.firstOrNull()
    }

    val webSuggestions = remember(query) {
        if (query.isBlank()) emptyList()
        else listOf(query, "$query store", "$query news", "$query india")
    }

    // Handle physical or gesture back action
    BackHandler {
        onDismiss()
    }

    // Auto-focus soft keyboard when overlay opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // Transparent overlay showing home wallpaper
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Pitch-Black Pill Search Bar Matching Image 2
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.Black, // Pitch Black surface
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search apps", color = Color.Gray, fontSize = 16.sp) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                    )

                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Results List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Showing Last Result on Empty Query (Replicating Image 2)
                if (query.isBlank() && lastResultApp != null) {
                    item {
                        SearchAppResultRow(
                            app = lastResultApp,
                            subText = "Last result",
                            onClick = {
                                val intent = context.packageManager.getLaunchIntentForPackage(lastResultApp.packageName)
                                intent?.let { context.startActivity(it) }
                                onDismiss()
                            }
                        )
                    }
                }

                items(filteredApps) { app ->
                    SearchAppResultRow(
                        app = app,
                        subText = "Tap Search to open",
                        onClick = {
                            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            intent?.let { context.startActivity(it) }
                            onDismiss()
                        }
                    )
                }

                if (webSuggestions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Web suggestions",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 4.dp)
                        )
                    }

                    items(webSuggestions) { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "•", color = Color(0xFF4DD0E1), fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = suggestion, color = Color.White, fontSize = 16.sp)
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "Search for more apps", color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAppResultRow(
    app: AppModel,
    subText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        app.icon?.let { iconDrawable ->
            Image(
                bitmap = iconDrawable.toBitmap(width = 44, height = 44).asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(text = subText, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}
