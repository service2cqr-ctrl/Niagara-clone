package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.ui.LauncherViewModel

@Composable
fun UsageBreakerDialog(
    viewModel: LauncherViewModel? = null,
    appName: String,
    launchCount: Int,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    val accentColorArgb = viewModel?.accentColorArgb?.collectAsState()?.value ?: 0xFF4DD0E1.toInt()
    val accentColor = Color(accentColorArgb)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Take a break?",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "You've opened $appName $launchCount times today. Consider taking a short pause.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(
                    text = "Open anyway",
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Take a break",
                    color = Color.Gray
                )
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF161616)
    )
}
