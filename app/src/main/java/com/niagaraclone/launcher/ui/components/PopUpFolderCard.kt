package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@Composable
fun PopUpFolderCard(
    folderName: String,
    subApps: List<AppModel>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF181818),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = folderName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            subApps.forEach { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            intent?.let { context.startActivity(it) }
                            onDismiss()
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    app.icon?.let { iconDrawable ->
                        Image(
                            bitmap = iconDrawable.toBitmap(width = 36, height = 36).asImageBitmap(),
                            contentDescription = app.label,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    Text(text = app.label, color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }
}
