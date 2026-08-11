package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToCategorySheet(
    app: AppModel,
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141414),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Add to category",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            CategoryRow(
                icon = Icons.Outlined.Headphones,
                title = "Music Apps",
                subtitle = "Appear when headphones or speakers are connected",
                onClick = {
                    viewModel.toggleMusicPackage(app.packageName)
                    onDismiss()
                }
            )

            CategoryRow(
                icon = Icons.Outlined.VisibilityOff,
                title = "Hidden apps",
                subtitle = "Tidy up your app list to find apps quicker",
                onClick = {
                    viewModel.toggleHidePackage(app.packageName)
                    onDismiss()
                }
            )

            CategoryRow(
                icon = Icons.Outlined.Security,
                title = "Private space",
                subtitle = "Keep sensitive apps isolated and protected",
                onClick = { onDismiss() }
            )

            CategoryRow(
                icon = Icons.Outlined.Folder,
                title = "social",
                subtitle = "WhatsApp, Gmail +5",
                onClick = { onDismiss() }
            )

            CategoryRow(
                icon = Icons.Outlined.Folder,
                title = "Finance",
                subtitle = "Finance watchlist, Paytm Money, Paytm +4",
                onClick = { onDismiss() }
            )

            CategoryRow(
                icon = Icons.Outlined.Folder,
                title = "productivity",
                subtitle = "At a Glance, Home Workout, Notes +3",
                onClick = { onDismiss() }
            )

            CategoryRow(
                icon = Icons.Outlined.CreateNewFolder,
                title = "Create pop-up folder",
                subtitle = null,
                onClick = { onDismiss() }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CategoryRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}
