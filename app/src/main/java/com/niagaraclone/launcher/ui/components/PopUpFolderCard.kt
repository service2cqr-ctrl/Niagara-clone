package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.services.MediaNotificationListener
import com.niagaraclone.launcher.services.NotificationSummaryModel
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.theme.LocalLauncherPalette
import com.niagaraclone.launcher.ui.theme.glassSurface

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PopUpFolderCard(
    viewModel: LauncherViewModel,
    parentPackage: String,
    folderName: String,
    subApps: List<AppModel>,
    appNotifications: List<NotificationSummaryModel> = emptyList(),
    onEditPopUp: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalLauncherPalette.current
    val shortcuts = remember(parentPackage) { viewModel.getAppShortcuts(parentPackage) }
    val quickRepliesEnabled by viewModel.quickRepliesEnabled.collectAsState()

    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val accentColor = Color(accentColorArgb)

    val hasAnyContent = appNotifications.isNotEmpty() || subApps.isNotEmpty() || shortcuts.isNotEmpty()
    val cardShape = RoundedCornerShape(26.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .shadow(elevation = 22.dp, shape = cardShape, clip = false)
            .glassSurface(shape = cardShape, isSheet = true, borderWidth = 1.3.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = onEditPopUp
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // --- HEADER ROW ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = folderName,
                        color = palette.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditPopUp,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Pop-up",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // --- 1. ACTIVE NOTIFICATIONS SECTION ---
            if (appNotifications.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    appNotifications.take(3).forEach { notif ->
                        var replyText by remember(notif.id) { mutableStateOf("") }
                        val notifShape = RoundedCornerShape(16.dp)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassSurface(shape = notifShape, isSheet = false, borderWidth = 1.dp)
                                .clickable {
                                    try {
                                        notif.sbn?.notification?.contentIntent?.send()
                                    } catch (e: Exception) {
                                        context.packageManager.getLaunchIntentForPackage(parentPackage)?.let {
                                            context.startActivity(it)
                                        }
                                    }
                                    onDismiss()
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = notif.title.ifEmpty { folderName },
                                        color = palette.textPrimary,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = {
                                            MediaNotificationListener.dismissNotification(notif.id)
                                            if (appNotifications.size <= 1 && subApps.isEmpty() && shortcuts.isEmpty()) {
                                                onDismiss()
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text("Dismiss", color = palette.textSecondary, fontSize = 11.sp)
                                    }
                                }

                                if (notif.text.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notif.text,
                                        color = palette.textSecondary,
                                        fontSize = 13.5.sp,
                                        lineHeight = 18.sp
                                    )
                                }

                                if (quickRepliesEnabled) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = replyText,
                                            onValueChange = { replyText = it },
                                            placeholder = {
                                                Text("Reply...", color = palette.textSecondary, fontSize = 12.sp)
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            textStyle = LocalTextStyle.current.copy(
                                                color = palette.textPrimary,
                                                fontSize = 13.sp
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = accentColor,
                                                unfocusedBorderColor = palette.borderColor
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = accentColor,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clickable {
                                                    if (replyText.isNotBlank()) {
                                                        MediaNotificationListener.sendDirectReply(notif.id, replyText)
                                                        replyText = ""
                                                        onDismiss()
                                                    }
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Send,
                                                    contentDescription = "Send Reply",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 2. SUB-APPS SECTION ---
            if (subApps.isNotEmpty()) {
                if (appNotifications.isNotEmpty()) {
                    HorizontalDivider(
                        color = palette.borderColor,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
                subApps.forEach { subApp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                context.packageManager.getLaunchIntentForPackage(subApp.packageName)?.let {
                                    context.startActivity(it)
                                }
                                onDismiss()
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        subApp.icon?.let { icon ->
                            Image(
                                bitmap = icon.toBitmap(width = 40, height = 40).asImageBitmap(),
                                contentDescription = subApp.label,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                        } ?: Spacer(modifier = Modifier.width(44.dp))

                        Text(
                            text = subApp.label,
                            color = palette.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // --- 3. APP SHORTCUTS SECTION ---
            if (shortcuts.isNotEmpty()) {
                if (appNotifications.isNotEmpty() || subApps.isNotEmpty()) {
                    HorizontalDivider(
                        color = palette.borderColor,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                shortcuts.take(4).forEach { shortcut ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.launchAppShortcut(shortcut.shortcutInfo)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        shortcut.icon?.let { icon ->
                            Image(
                                bitmap = icon.toBitmap(width = 36, height = 36).asImageBitmap(),
                                contentDescription = shortcut.label,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        } ?: Spacer(modifier = Modifier.width(40.dp))

                        Text(
                            text = shortcut.label,
                            color = palette.textPrimary.copy(alpha = 0.9f),
                            fontSize = 14.5.sp
                        )
                    }
                }
            }

            // --- 4. NIAGARA EMPTY-STATE POP-UP MESSAGE ---
            if (!hasAnyContent) {
                Text(
                    text = buildAnnotatedString {
                        append("Notifications appear here.\nLong-press this pop-up and tap on ")
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = palette.textPrimary)) {
                            append("Edit $folderName pop-up")
                        }
                        append(" to add apps, shortcuts or a widget.")
                    },
                    color = palette.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditPopUp() }
                        .padding(vertical = 6.dp)
                )
            }
        }
    }
}
