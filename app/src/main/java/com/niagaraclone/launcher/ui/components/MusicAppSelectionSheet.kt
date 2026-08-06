package com.niagaraclone.launcher.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SwapVert
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

private enum class MusicSheetStep { EXPLANATION, SELECTION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicAppSelectionSheet(
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(MusicSheetStep.EXPLANATION) }

    val apps by viewModel.apps.collectAsState()
    val selectedPackages by viewModel.selectedMusicPackages.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Black, // Pitch Black Background
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        AnimatedContent(
            targetState = step,
            label = "music_sheet_transition",
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { currentStep ->
            when (currentStep) {
                MusicSheetStep.EXPLANATION -> MusicExplanationStep(
                    onSelectAppsClick = { step = MusicSheetStep.SELECTION },
                    onCancelClick = onDismiss
                )
                MusicSheetStep.SELECTION -> MusicSelectionStep(
                    apps = apps,
                    selectedPackages = selectedPackages,
                    onTogglePackage = { pkg -> viewModel.toggleMusicPackage(pkg) },
                    onDone = onDismiss
                )
            }
        }
    }
}

@Composable
private fun MusicExplanationStep(
    onSelectAppsClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Launch your music apps right when you need them",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            text = "Select your favourite music, podcast, or other apps to automatically appear on the home screen when connecting speakers or headphones.",
            fontSize = 14.sp,
            color = Color.Gray,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Niagara Launcher needs the nearby devices permission to show your music apps on the home screen when connecting headphones or speakers.",
            fontSize = 13.sp,
            color = Color.Gray.copy(alpha = 0.8f),
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onSelectAppsClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
            Text(text = "Select apps", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onCancelClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Cancel", fontSize = 16.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MusicSelectionStep(
    apps: List<AppModel>,
    selectedPackages: Set<String>,
    onTogglePackage: (String) -> Unit,
    onDone: () -> Unit
) {
    val selectedApps = remember(apps, selectedPackages) {
        apps.filter { selectedPackages.contains(it.packageName) }
    }

    val suggestedApps = remember(apps, selectedPackages) {
        apps.filter { !selectedPackages.contains(it.packageName) && (it.label.contains("Music", ignoreCase = true) || it.label.contains("Spotify", ignoreCase = true) || it.label.contains("YouTube", ignoreCase = true)) }
    }

    val otherApps = remember(apps, selectedPackages) {
        apps.filter { !selectedPackages.contains(it.packageName) && !suggestedApps.contains(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Music Apps",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Appear when headphones or speakers are connected",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedApps.isNotEmpty()) {
                    item {
                        Text(text = "Selected", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(selectedApps) { app ->
                        MusicAppRowItem(
                            app = app,
                            isSelected = true,
                            onToggle = { onTogglePackage(app.packageName) },
                            showDragHandle = true
                        )
                    }

                    item {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF222222),
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .clickable { }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.SwapVert, contentDescription = null, tint = Color(0xFF4DD0E1), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Sort apps by usage", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }

                if (suggestedApps.isNotEmpty()) {
                    item {
                        Text(text = "Suggestions", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    }
                    items(suggestedApps) { app ->
                        MusicAppRowItem(
                            app = app,
                            isSelected = false,
                            onToggle = { onTogglePackage(app.packageName) },
                            showDragHandle = false
                        )
                    }
                }

                item {
                    Text(text = "All Apps", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                }

                items(otherApps) { app ->
                    MusicAppRowItem(
                        app = app,
                        isSelected = false,
                        onToggle = { onTogglePackage(app.packageName) },
                        showDragHandle = false
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Sticky Confirm Button
        Button(
            onClick = onDone,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .height(48.dp)
                .width(140.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.Black, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Done", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MusicAppRowItem(
    app: AppModel,
    isSelected: Boolean,
    onToggle: () -> Unit,
    showDragHandle: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4DD0E1), uncheckedColor = Color.Gray)
        )
        Spacer(modifier = Modifier.width(8.dp))

        app.icon?.let { iconDrawable ->
            Image(
                bitmap = iconDrawable.toBitmap(width = 36, height = 36).asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Text(
            text = app.label,
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )

        if (showDragHandle) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
