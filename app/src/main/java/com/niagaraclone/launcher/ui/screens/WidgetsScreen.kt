package com.niagaraclone.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Hosts pinned widgets (swipe-left page in Niagara). Wiring real AppWidgetHost
 * requires a BIND_APPWIDGET-holding service; this screen is the drop-in point —
 * add an AppWidgetHostView per saved widget ID here once that service exists.
 */
@Composable
fun WidgetsScreen() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Widgets", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Long-press here to add a widget",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
