package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CascadiaButtonMenuSheet(
    viewModel: LauncherViewModel,
    onEditTapAction: () -> Unit,
    onEditSwipeAction: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(text = "Cascadia Button", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))

            SheetRowItem(
                icon = Icons.Outlined.Edit,
                title = "Edit tap action",
                onClick = {
                    onDismiss()
                    onEditTapAction()
                }
            )

            SheetRowItem(
                icon = Icons.Outlined.Edit,
                title = "Edit swipe up action",
                onClick = {
                    onDismiss()
                    onEditSwipeAction()
                }
            )

            SheetRowItem(
                icon = Icons.Outlined.StarBorder,
                title = "Enable Pro badge",
                onClick = { onDismiss() }
            )

            SheetRowItem(
                icon = Icons.Default.Settings,
                title = "Cascadia settings",
                onClick = {
                    onDismiss()
                    onOpenSettings()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FabActionSelectionSheet(
    viewModel: LauncherViewModel,
    apps: List<AppModel>,
    isSwipeAction: Boolean = false,
    onDismiss: () -> Unit
) {
    val fabActionType by (if (isSwipeAction) viewModel.fabSwipeActionType else viewModel.fabTapActionType).collectAsState()
    val fabPackage by (if (isSwipeAction) viewModel.fabSwipePackage else viewModel.fabTapPackage).collectAsState()
    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val accentColor = Color(accentColorArgb)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (isSwipeAction) "Select Swipe Up Action" else "Select Tap Action",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(text = "Suggestions", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                }

                item {
                    ActionOptionRow(
                        title = "Phone",
                        icon = Icons.Default.Phone,
                        isSelected = fabActionType == "DIAL",
                        accentColor = accentColor,
                        onClick = {
                            if (isSwipeAction) viewModel.setFabSwipeAction("DIAL") else viewModel.setFabTapAction("DIAL")
                            onDismiss()
                        }
                    )
                }

                item {
                    ActionOptionRow(
                        title = "Search",
                        icon = Icons.Default.Search,
                        isSelected = fabActionType == "SEARCH",
                        accentColor = accentColor,
                        onClick = {
                            if (isSwipeAction) viewModel.setFabSwipeAction("SEARCH") else viewModel.setFabTapAction("SEARCH")
                            onDismiss()
                        }
                    )
                }

                item {
                    ActionOptionRow(
                        title = "Your agenda",
                        icon = Icons.Outlined.CalendarToday,
                        isSelected = fabActionType == "AGENDA",
                        accentColor = accentColor,
                        onClick = {
                            if (isSwipeAction) viewModel.setFabSwipeAction("AGENDA") else viewModel.setFabTapAction("AGENDA")
                            onDismiss()
                        }
                    )
                }

                item {
                    ActionOptionRow(
                        title = "Voice Assistant",
                        icon = Icons.Outlined.Mic,
                        isSelected = fabActionType == "VOICE_ASSISTANT",
                        accentColor = accentColor,
                        onClick = {
                            if (isSwipeAction) viewModel.setFabSwipeAction("VOICE_ASSISTANT") else viewModel.setFabTapAction("VOICE_ASSISTANT")
                            onDismiss()
                        }
                    )
                }

                item {
                    ActionOptionRow(
                        title = "Lock screen",
                        icon = Icons.Default.Lock,
                        isSelected = fabActionType == "LOCK",
                        accentColor = accentColor,
                        onClick = {
                            if (isSwipeAction) viewModel.setFabSwipeAction("LOCK") else viewModel.setFabTapAction("LOCK")
                            onDismiss()
                        }
                    )
                }

                item {
                    Text(text = "Applications", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                }

                items(apps) { app ->
                    val isAppSelected = fabActionType == "APP" && fabPackage == app.packageName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSwipeAction) viewModel.setFabSwipeAction("APP", app.packageName) else viewModel.setFabTapAction("APP", app.packageName)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        app.icon?.let { iconDrawable ->
                            Image(
                                bitmap = iconDrawable.toBitmap(width = 40, height = 40).asImageBitmap(),
                                contentDescription = app.label,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Text(text = app.label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        if (isAppSelected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = accentColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionOptionRow(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (isSelected) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = accentColor)
        }
    }
}

@Composable
private fun SheetRowItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}
