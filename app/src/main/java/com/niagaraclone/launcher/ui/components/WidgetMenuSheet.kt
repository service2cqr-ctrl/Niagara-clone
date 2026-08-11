package com.niagaraclone.launcher.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
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
import kotlin.math.roundToInt

private enum class WidgetSubPage {
    MAIN, CLOCK_STYLE, CALENDAR, CALENDAR_FILTER, WEATHER
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun WidgetMenuSheet(
    viewModel: LauncherViewModel,
    onAddWidgetClick: () -> Unit,
    onMoveWidgetClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEditFavorites: () -> Unit,
    onDismiss: () -> Unit
) {
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141414),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = { fadeIn() with fadeOut() },
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
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        WidgetMenuItemRow(
                            icon = Icons.Outlined.Schedule,
                            title = "Clock style",
                            onClick = { currentPage = WidgetSubPage.CLOCK_STYLE }
                        )

                        WidgetMenuItemRow(
                            icon = Icons.Outlined.CalendarToday,
                            title = "Calendar",
                            subtitle = "Show upcoming events, Show date",
                            onClick = { currentPage = WidgetSubPage.CALENDAR }
                        )

                        WidgetMenuItemToggleRow(
                            icon = Icons.Outlined.WbSunny,
                            title = "Weather",
                            subtitle = "Location, Temperature units",
                            checked = weatherEnabled,
                            onCheckedChange = { viewModel.setWeatherEnabled(it) },
                            onClick = { currentPage = WidgetSubPage.WEATHER }
                        )

                        WidgetMenuItemRow(
                            icon = Icons.Outlined.AddBox,
                            title = "Add Custom Widget",
                            onClick = {
                                onDismiss()
                                onAddWidgetClick()
                            }
                        )

                        WidgetMenuItemRow(
                            icon = Icons.Outlined.UnfoldMore,
                            title = "Move Widget",
                            subtitle = "Adjust header vertical position",
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
                            onCheckedChange = { viewModel.setShowMediaWidget(it) }
                        )

                        WidgetMenuItemToggleRow(
                            icon = Icons.Outlined.BatteryStd,
                            title = "Show Battery Percentage",
                            checked = showBatteryPercentage,
                            onCheckedChange = { viewModel.setShowBatteryPercentage(it) }
                        )

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        WidgetMenuItemRow(
                            icon = Icons.Outlined.Star,
                            title = "Edit favourites",
                            onClick = {
                                onDismiss()
                                onOpenEditFavorites()
                            }
                        )

                        WidgetMenuItemRow(
                            icon = Icons.Outlined.Settings,
                            title = "Cascadia settings",
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
                        onSelectStyle = {
                            viewModel.setClockStyle(it)
                            currentPage = WidgetSubPage.MAIN
                        }
                    )
                }

                WidgetSubPage.CALENDAR -> {
                    CalendarSettingsSubPage(
                        previewEnabled = calendarPreviewEnabled,
                        onTogglePreview = { viewModel.setCalendarPreviewEnabled(it) },
                        onOpenFilter = { currentPage = WidgetSubPage.CALENDAR_FILTER }
                    )
                }

                WidgetSubPage.CALENDAR_FILTER -> {
                    CalendarFilterSubPage(
                        onClose = { currentPage = WidgetSubPage.CALENDAR }
                    )
                }

                WidgetSubPage.WEATHER -> {
                    WeatherSettingsSubPage(
                        weatherEnabled = weatherEnabled,
                        tempUnit = tempUnit,
                        liveTemp = liveWeather?.temperatureCelsius ?: 28f,
                        accentColor = accentColor,
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
            Text(text = "Clock style", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                    color = if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0xFF222222),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) accentColor else Color.Transparent,
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
                            color = if (isSelected) accentColor else Color.White
                        )
                        Text(text = title, fontSize = 12.sp, color = Color.Gray)
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
    onTogglePreview: (Boolean) -> Unit,
    onOpenFilter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text = "Calendar", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))

        WidgetMenuItemToggleRow(
            icon = Icons.Outlined.CalendarToday,
            title = "Show upcoming events",
            checked = previewEnabled,
            onCheckedChange = onTogglePreview
        )

        WidgetMenuItemRow(
            icon = Icons.Outlined.FilterList,
            title = "Filter events",
            onClick = onOpenFilter
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun CalendarFilterSubPage(
    onClose: () -> Unit
) {
    val sampleAccounts = listOf(
        Pair("Events", Color(0xFF4DD0E1)),
        Pair("Holidays in India", Color(0xFF81C784)),
        Pair("Samsung Calendar", Color(0xFFBA68C8))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text = "Filter events", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))

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
                Text(text = name, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { isChecked = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4DD0E1))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DD0E1), contentColor = Color.Black),
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
    onToggleWeather: (Boolean) -> Unit,
    onSelectTempUnit: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text = "Weather", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF222222),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Current location", fontSize = 12.sp, color = Color.Gray)
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
            Text("Temperature units", color = Color.White, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = tempUnit == "CELSIUS",
                    onClick = { onSelectTempUnit("CELSIUS") },
                    label = { Text("°C") }
                )
                FilterChip(
                    selected = tempUnit == "FAHRENHEIT",
                    onClick = { onSelectTempUnit("FAHRENHEIT") },
                    label = { Text("°F") }
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
                containerColor = if (weatherEnabled) Color(0xFF333333) else accentColor,
                contentColor = if (weatherEnabled) Color.White else Color.Black
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun WidgetMenuItemToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
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
        Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF4DD0E1))
        )
    }
}
