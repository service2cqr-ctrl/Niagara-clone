package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.theme.LocalLauncherPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserGuideSheet(
    viewModel: LauncherViewModel? = null,
    onDismiss: () -> Unit
) {
    val palette = LocalLauncherPalette.current
    val accentColorArgb = viewModel?.accentColorArgb?.collectAsState()?.value ?: 0xFF4DD0E1.toInt()
    val accentColor = Color(accentColorArgb)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.sheetBackground,
        contentColor = palette.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Cascadia User Guide",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Text(
                text = "Master your home screen gestures and features",
                color = palette.textSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            GuideCard(
                icon = Icons.Outlined.Swipe,
                title = "Wave Alphabet Indexer",
                description = "Slide your finger along the right edge of the screen to quickly jump to any app letter. Dragging inward expands a curved wave.",
                accentColor = accentColor,
                palette = palette
            )

            GuideCard(
                icon = Icons.Outlined.Folder,
                title = "Pop-up Folders",
                description = "Swipe right on any favorite app row to reveal its pop-up folder and default system shortcuts.",
                accentColor = accentColor,
                palette = palette
            )

            GuideCard(
                icon = Icons.Outlined.TouchApp,
                title = "Cascadia Button",
                description = "Tap the floating action button to trigger your primary quick action (e.g. Phone). Swipe up on the button to perform your secondary shortcut (e.g. Search).",
                accentColor = accentColor,
                palette = palette
            )

            GuideCard(
                icon = Icons.Outlined.Lock,
                title = "Quick Lock",
                description = "Double-tap any empty space on your home screen to instantly turn off and lock your display.",
                accentColor = accentColor,
                palette = palette
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GuideCard(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    palette: com.niagaraclone.launcher.ui.theme.LauncherPalette
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    color = palette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
