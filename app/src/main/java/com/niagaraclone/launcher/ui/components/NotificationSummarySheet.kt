package com.niagaraclone.launcher.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.services.MediaNotificationListener
import com.niagaraclone.launcher.services.NotificationSummaryModel
import com.niagaraclone.launcher.ui.theme.LocalLauncherPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSummarySheet(
    summaries: List<NotificationSummaryModel>,
    accentColor: Color = Color(0xFF4DD0E1),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalLauncherPalette.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.sheetBackground,
        contentColor = palette.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = "Notification summary",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                if (summaries.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            summaries.forEach { MediaNotificationListener.dismissNotification(it.id) }
                        }
                    ) {
                        Text("Clear all", color = accentColor, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (summaries.isEmpty()) {
                Text(
                    text = "No active notifications.",
                    color = palette.textSecondary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .padding(bottom = 24.dp)
                ) {
                    items(
                        items = summaries,
                        key = { it.id }
                    ) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.cardBackground, RoundedCornerShape(14.dp))
                                .border(1.dp, palette.borderColor, RoundedCornerShape(14.dp))
                                .clickable {
                                    try {
                                        val intent = context.packageManager.getLaunchIntentForPackage(item.packageName)
                                        intent?.let {
                                            context.startActivity(it)
                                            onDismiss()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .padding(14.dp)
                        ) {
                            Text(
                                text = item.title,
                                color = palette.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.text.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.text,
                                    color = palette.textSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
