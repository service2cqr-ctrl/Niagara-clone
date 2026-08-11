package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UsageBreakerDialog(
    appName: String,
    launchCount: Int,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Take a break?", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "You've opened $appName $launchCount times today. Consider taking a short pause.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 15.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(text = "Open Anyway", color = Color(0xFF4DD0E1))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Take a Break", color = Color.Gray)
            }
        },
        containerColor = Color.Black
    )
}
