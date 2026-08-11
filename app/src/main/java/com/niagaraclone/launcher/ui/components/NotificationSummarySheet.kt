package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.services.NotificationSummaryModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSummarySheet(
    summaries: List<NotificationSummaryModel>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Notification Summary",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (summaries.isEmpty()) {
                Text(
                    text = "No active notifications.",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(summaries) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(text = item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            if (item.text.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = item.text, color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
