package com.niagaraclone.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
import kotlin.math.roundToInt

private enum class WidgetSubPage {
    MAIN, CLOCK_STYLE, CALENDAR, CALENDAR_FILTER, WEATHER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetMenuSheet(
    viewModel: LauncherViewModel,
    onAddWidgetClick: () -> Unit,
    onMoveWidgetClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEditFavorites: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalLauncherPalette.current
    var currentPage by remember { mutableStateOf(WidgetSubPage.MAIN) }

    val clockStyle by viewModel.clockStyle.collectAsState()
    val weatherEnabled by viewModel.weatherEnabled.collectAsState()
    val tempUnit by viewModel.weatherTempUnit.collectAsState()
    val liveWeather by viewModel.liveWeather.collectAsState()
    val rawShowMediaWidget by viewModel.showMediaWidget.collectAsState()
    val showBatteryPercentage by viewModel.showBatteryPercentage.collectAsState()
    val calendarPreviewEnabled by viewModel.repository.calendarPreviewEnabled.collectAsState(initial = true)
    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val accentColor = Color(accentColorArgb)

    // Handle back button inside sub-pages gracefully
    BackHandler(enabled = currentPage != WidgetSubPage.MAIN) {
        currentPage = when (currentPage) {
            WidgetSubPage.CALENDAR_FILTER -> WidgetSubPage.CALENDAR
            else -> WidgetSubPage.MAIN
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.sheetBackground,
        contentColor = palette.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "WidgetMenuAnimation"
        ) { page ->
            when (page) {
                WidgetSubPage.MAIN -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Cascadia widget",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        WidgetMenuItemRow(
                            icon = Icons.Outlined.Schedule,
                            title = "Clock style",
                            palette = palette,
                            onClick = { currentPage = WidgetSubPage.CLOCK_STYLE }
                        )

                        WidgetMenuItemRow(
                            icon = Icons.Outlined.CalendarToday,
                            title = "Calendar",
                            subtitle = "Show upcoming events, Show date",
                            palette = palette,
                            onClick = { currentPage = WidgetSubPage.CALENDAR }
                        )

                        WidgetMenuItemToggleRow(
                            icon = Icons.Outlined.WbSunny,
                            title = "Weather",
                            subtitle = "Location, Temperature units",
                            checked = weatherEnabled,
                            accentColor = accentColor,
                            palette = palette,
                            onCheckedChange = { viewModel.setWeatherEnabled(it) },
                            onClick = { currentPage = WidgetSubPage.WEATHER }
                        )

                        WidgetMenuItemRow(
                            icon = Icons.Outlined.AddBox,
                            title = "Add Custom Widget",
                            palette = palette,
                            onClick = {
                                onDismiss()
                                onAddWidgetClick()
                            }
                        )

                        WidgetMenuItemRow(
                            icon = Icons.Outlined.UnfoldMore,
                            title = "Move Widget",
                            subtitle = "Adjust header vertical position",
                            palette = palette,
                            onClick = {
                                onDismiss()
                                onMoveWidgetClick()
                            }
                        )

                        WidgetMenuItemToggleRow(
                            icon = Icons.Outlined.MusicNote,
                            title = "Show media widget",
                            subtitle = "While playing music",
                            checked = rawShowMediaWidget,
                            accentColor = accentColor,
                            palette = palette,
                            onCheckedChange = { viewModel.setShowMediaWidget(it) }
                        )

                        WidgetMenuItemToggleRow(
                            icon = Icons.Outlined.BatteryStd,
                            title = "Show Battery Percentage",
                            checked = showBatteryPercentage,
                            accentColor = accentColor,
                            palette = palette,
                            onCheckedChange = { viewModel.setShowBatteryPercentage(it) }
                        )

                        HorizontalDivider(
                            color = palette.borderColor,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        WidgetMenuItemRow(
                            icon = Icons.Outlined.Star,
                            title = "Edit favourites",
                            palette = palette,
                            onClick = {
                                onDismiss()
                                onOpenEditFavorites()
                            }
                        )

                        WidgetMenuItemRow(
                            icon = Icons.Outlined.Settings,
                            title = "Cascadia settings",
                            palette = palette,
                            onClick = {
                                onDismiss()
                                onOpenSettings()
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                WidgetSubPage.CLOCK_STYLE -> {
                    ClockStyleSubPage(
                        selectedStyle = clockStyle,
                        accentColor = accentColor,
                        palette = palette,
                        onSelectStyle = {
                            viewModel.setClockStyle(it)
                            currentPage = WidgetSubPage.MAIN
                        }
                    )
                }

                WidgetSubPage.CALENDAR -> {
                    CalendarSettingsSubPage(
                        previewEnabled = calendarPreviewEnabled,
                        accentColor = accentColor,
                        palette = palette,
                        onTogglePreview = { viewModel.setCalendarPreviewEnabled(it) },
                        onOpenFilter = { currentPage = WidgetSubPage.CALENDAR_FILTER }
                    )
                }

                WidgetSubPage.CALENDAR_FILTER -> {
                    CalendarFilterSubPage(
                        accentColor = accentColor,
                        palette = palette,
                        onClose = { currentPage = WidgetSubPage.CALENDAR }
                    )
                }

                WidgetSubPage.WEATHER -> {
                    WeatherSettingsSubPage(
                        weatherEnabled = weatherEnabled,
                        tempUnit = tempUnit,
                        liveTemp = liveWeather?.temperatureCelsius ?: 28f,
                        accentColor = accentColor,
                        palette = palette,
                        onToggleWeather = { viewModel.setWeatherEnabled(it) },
                        onSelectTempUnit = { viewModel.setWeatherTempUnit(it) },
                        onClose = { currentPage = WidgetSubPage.MAIN }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClockStyleSubPage(
    selectedStyle: String,
    accentColor: Color,
    palette: com.niagaraclone.launcher.ui.theme.LauncherPalette,
    onSelectStyle: (String) -> Unit
) {
    val styles = listOf(
        Pair("DIGITAL_HEADER", "Digital Header"),
        Pair("DIGITAL_MINIMAL", "Digital Minimal"),
        Pair("SPLIT_COLORS", "Split Color Accent")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "Clock style", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().height(260.dp)
        ) {
            items(styles) { (styleKey, title) ->
                val isSelected = styleKey == selectedStyle
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) accentColor.copy(alpha = 0.2f) else palette.cardBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) accentColor else palette.borderColor,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectStyle(styleKey) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (styleKey == "DIGITAL_MINIMAL") "10:28" else "10 28",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) accentColor else palette.textPrimary
                        )
                        Text(text = title, fontSize = 12.sp, color = palette.textSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun CalendarSettingsSubPage(
    previewEnabled: Boolean,
    accentColor: Color,
    palette: com.niagaraclone.launcher.ui.theme.LauncherPalette,
    onTogglePreview: (Boolean) -> Unit,
    onOpenFilter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text = "Calendar", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary, modifier = Modifier.padding(bottom = 16.dp))

        WidgetMenuItemToggleRow(
            icon = Icons.Outlined.CalendarToday,
            title = "Show upcoming events",
            checked = previewEnabled,
            accentColor = accentColor,
            palette = palette,
            onCheckedChange = onTogglePreview
        )

        WidgetMenuItemRow(
            icon = Icons.Outlined.FilterList,
            title = "Filter events",
            palette = palette,
            onClick = onOpenFilter
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun CalendarFilterSubPage(
    accentColor: Color,
    palette: com.niagaraclone.launcher.ui.theme.LauncherPalette,
    onClose: () -> Unit
) {
    val sampleAccounts = listOf(
        Pair("Events", accentColor),
        Pair("Holidays in India", Color(0xFF81C784)),
        Pair("Samsung Calendar", Color(0xFFBA68C8))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text = "Filter events", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary, modifier = Modifier.padding(bottom = 16.dp))

        sampleAccounts.forEach { (name, dotColor) ->
            var isChecked by remember { mutableStateOf(true) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isChecked = !isChecked }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).background(dotColor, RoundedCornerShape(5.dp)))
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = name, color = palette.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { isChecked = it },
                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Okay", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun WeatherSettingsSubPage(
    weatherEnabled: Boolean,
    tempUnit: String,
    liveTemp: Float,
    accentColor: Color,
    palette: com.niagaraclone.launcher.ui.theme.LauncherPalette,
    onToggleWeather: (Boolean) -> Unit,
    onSelectTempUnit: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text = "Weather", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary, modifier = Modifier.padding(bottom = 16.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.cardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.borderColor),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Current location", fontSize = 12.sp, color = palette.textSecondary)
                    Text(
                        text = if (tempUnit == "CELSIUS") "${liveTemp.roundToInt()}° C" else "${(liveTemp * 9 / 5 + 32).roundToInt()}° F",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = accentColor, modifier = Modifier.size(40.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Temperature units", color = palette.textPrimary, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = tempUnit == "CELSIUS",
                    onClick = { onSelectTempUnit("CELSIUS") },
                    label = { Text("°C") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor.copy(alpha = 0.3f))
                )
                FilterChip(
                    selected = tempUnit == "FAHRENHEIT",
                    onClick = { onSelectTempUnit("FAHRENHEIT") },
                    label = { Text("°F") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onToggleWeather(!weatherEnabled)
                onClose()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (weatherEnabled) palette.cardBackground else accentColor,
                contentColor = if (weatherEnabled) palette.textPrimary else Color.Black
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (weatherEnabled) "Disable weather" else "Enable weather", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun WidgetMenuItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    palette: com.niagaraclone.launcher.ui.theme.LauncherPalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = palette.textPrimary.copy(alpha = 0.9f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = palette.textPrimary, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 12.sp, color = palette.textSecondary)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun WidgetMenuItemToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    accentColor: Color,
    palette: com.niagaraclone.launcher.ui.theme.LauncherPalette,
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick?.invoke() ?: onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = palette.textPrimary.copy(alpha = 0.9f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = palette.textPrimary, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 12.sp, color = palette.textSecondary)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = palette.textSecondary,
                uncheckedTrackColor = palette.cardBackground
            )
        )
    }
}
