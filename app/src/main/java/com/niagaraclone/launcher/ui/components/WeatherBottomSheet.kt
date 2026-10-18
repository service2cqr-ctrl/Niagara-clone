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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.theme.LocalLauncherPalette
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun rememberCurrentTimeState(
    timeFormatPattern: String = "h:mm",
    dateFormatPattern: String = "EEE d MMM"
): Pair<String, String> {
    var timeStr by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            timeStr = SimpleDateFormat(timeFormatPattern, Locale.getDefault()).format(now)
            dateStr = SimpleDateFormat(dateFormatPattern, Locale.getDefault()).format(now)
            delay(1000L)
        }
    }

    return Pair(timeStr, dateStr)
}

private enum class WeatherStep {
    MAIN,
    CHOOSE_LOCATION,
    SCANNING_CURRENT_LOCATION,
    FIXED_LOCATION_PROMPT,
    MANUAL_LOCATION_INPUT,
    MANUAL_LOCATION_CONFIRM,
    TEMP_UNITS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherBottomSheet(
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit
) {
    val palette = LocalLauncherPalette.current
    var currentStep by remember { mutableStateOf(WeatherStep.MAIN) }

    val weatherEnabled by viewModel.weatherEnabled.collectAsState()
    val locationMode by viewModel.weatherLocationMode.collectAsState()
    val cityName by viewModel.weatherCityName.collectAsState()
    val tempUnit by viewModel.weatherTempUnit.collectAsState()
    val liveWeather by viewModel.liveWeather.collectAsState()
    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val accentColor = Color(accentColorArgb)

    var tempSearchCity by remember { mutableStateOf("") }

    val tempDisplay = remember(liveWeather, tempUnit) {
        val c = liveWeather?.temperatureCelsius ?: 28f
        if (tempUnit == "CELSIUS") "${c.roundToInt()}°" else "${(c * 9 / 5 + 32).roundToInt()}°"
    }

    val conditionIcon = remember(liveWeather) {
        liveWeather?.conditionIcon ?: "☀️"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.sheetBackground,
        contentColor = palette.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        AnimatedContent(
            targetState = currentStep,
            label = "weather_step_transition",
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { step ->
            when (step) {
                WeatherStep.MAIN -> MainWeatherStep(
                    locationText = if (locationMode == "AUTO") "Current location · $cityName" else cityName,
                    tempUnitText = if (tempUnit == "CELSIUS") "Celsius" else "Fahrenheit",
                    tempDisplay = "$conditionIcon $tempDisplay",
                    accentColor = accentColor,
                    palette = palette,
                    onOpenLocation = { currentStep = WeatherStep.CHOOSE_LOCATION },
                    onOpenTempUnits = { currentStep = WeatherStep.TEMP_UNITS },
                    onKeepEnabled = {
                        viewModel.setWeatherEnabled(true)
                        onDismiss()
                    },
                    onDisable = {
                        viewModel.setWeatherEnabled(false)
                        onDismiss()
                    }
                )

                WeatherStep.CHOOSE_LOCATION -> ChooseLocationStep(
                    selectedMode = locationMode,
                    accentColor = accentColor,
                    palette = palette,
                    onSelectAuto = { currentStep = WeatherStep.SCANNING_CURRENT_LOCATION },
                    onSelectFixed = { currentStep = WeatherStep.FIXED_LOCATION_PROMPT }
                )

                WeatherStep.SCANNING_CURRENT_LOCATION -> ScanningCurrentLocationStep(
                    viewModel = viewModel,
                    accentColor = accentColor,
                    palette = palette,
                    onScanComplete = {
                        viewModel.setWeatherLocationMode("AUTO")
                        currentStep = WeatherStep.MAIN
                    }
                )

                WeatherStep.FIXED_LOCATION_PROMPT -> FixedLocationPromptStep(
                    accentColor = accentColor,
                    palette = palette,
                    onOkayAutoDetect = { currentStep = WeatherStep.SCANNING_CURRENT_LOCATION },
                    onEnterManually = { currentStep = WeatherStep.MANUAL_LOCATION_INPUT }
                )

                WeatherStep.MANUAL_LOCATION_INPUT -> ManualLocationInputStep(
                    accentColor = accentColor,
                    palette = palette,
                    onCitySelected = { city ->
                        tempSearchCity = city
                        currentStep = WeatherStep.MANUAL_LOCATION_CONFIRM
                    }
                )

                WeatherStep.MANUAL_LOCATION_CONFIRM -> ManualLocationConfirmStep(
                    city = tempSearchCity,
                    accentColor = accentColor,
                    palette = palette,
                    onConfirm = {
                        viewModel.setWeatherLocationMode("FIXED")
                        viewModel.setWeatherCityName(tempSearchCity)
                        viewModel.fetchLiveWeatherForCurrentMode()
                        currentStep = WeatherStep.MAIN
                    },
                    onCancel = { currentStep = WeatherStep.MAIN }
                )

                WeatherStep.TEMP_UNITS -> TempUnitsStep(
                    selectedUnit = tempUnit,
                    accentColor = accentColor,
                    palette = palette,
                    onSelectUnit = { unit ->
                        viewModel.setWeatherTempUnit(unit)
                        currentStep = WeatherStep.MAIN
                    }
                )
            }
        }
    }
}

@Composable
private fun MainWeatherStep(
    locationText: String,
    tempUnitText: String,
    tempDisplay: String,
    accentColor: Color,
    palette: com.niagaraclone.launcher.ui.theme.LauncherPalette,
    onOpenLocation: () -> Unit,
    onOpenTempUnits: () -> Unit,
    onKeepEnabled: () -> Unit,
    onDisable: () -> Unit
) {
    val (currentTime, currentDate) = rememberCurrentTimeState(timeFormatPattern = "h:mm", dateFormatPattern = "EEE d MMM")

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp)) {
            Icon(imageVector = Icons.Default.WbSunny, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Weather", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.cardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.borderColor),
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = currentTime.ifEmpty { "10:28" }, fontSize = 42.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = currentDate.ifEmpty { "Wed 5 Aug" }, fontSize = 16.sp, color = palette.textSecondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = tempDisplay, fontSize = 18.sp, color = accentColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenLocation).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Location", fontSize = 16.sp, color = palette.textPrimary)
                Text(text = locationText, fontSize = 13.sp, color = palette.textSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = palette.textSecondary)
        }

        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenTempUnits).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Style, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Temperature units", fontSize = 16.sp, color = palette.textPrimary)
                Text(text = tempUnitText, fontSize = 13.sp, color = palette.textSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = palette.textSecondary)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Tapping the weather widget reveals hourly and daily forecast. Learn more", fontSize = 12.sp, color = palette.textSecondary)
        Text(text = "Powered by Open-Meteo", fontSize = 12.sp, color = palette.textSecondary, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

        Button(onClick = onKeepEnabled, modifier = Modifier.fillMaxWidth().height(52.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)) {
            Text(text = "Keep enabled", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onDisable, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Disable", fontSize = 16.sp, color = palette.textPrimary)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ChooseLocationStep(selectedMode: String, accentColor: Color, palette: com.niagaraclone.launcher.ui.theme.LauncherPalette, onSelectAuto: () -> Unit, onSelectFixed: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(text = "Choose location", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "We share this location with our weather provider to give you a regional forecast. We don't use it for anything else.", fontSize = 13.sp, color = palette.textSecondary)
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onSelectAuto).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Current location", fontSize = 16.sp, color = palette.textPrimary)
                Text(text = "Updates automatically", fontSize = 13.sp, color = palette.textSecondary)
            }
            RadioButton(selected = (selectedMode == "AUTO"), onClick = onSelectAuto, colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = palette.textSecondary))
        }

        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onSelectFixed).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) { Text(text = "Fixed location", fontSize = 16.sp, color = palette.textPrimary) }
            RadioButton(selected = (selectedMode == "FIXED"), onClick = onSelectFixed, colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = palette.textSecondary))
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ScanningCurrentLocationStep(
    viewModel: LauncherViewModel,
    accentColor: Color,
    palette: com.niagaraclone.launcher.ui.theme.LauncherPalette,
    onScanComplete: () -> Unit
) {
    var isScanned by remember { mutableStateOf(false) }
    val cityName by viewModel.weatherCityName.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchLiveWeatherForCurrentMode()
        delay(1000)
        isScanned = true
        delay(800)
        onScanComplete()
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(text = "Current location", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.cardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.borderColor),
            modifier = Modifier.fillMaxWidth().height(110.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (!isScanned) {
                    CircularProgressIndicator(color = accentColor, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = cityName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FixedLocationPromptStep(accentColor: Color, palette: com.niagaraclone.launcher.ui.theme.LauncherPalette, onOkayAutoDetect: () -> Unit, onEnterManually: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(text = "Fixed location", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Do you want us to detect your location automatically? This generally gives more accurate forecasts than entering it manually.", fontSize = 14.sp, color = palette.textSecondary)
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onOkayAutoDetect, modifier = Modifier.fillMaxWidth().height(52.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)) {
            Text(text = "Okay", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onEnterManually, modifier = Modifier.fillMaxWidth()) { Text(text = "Enter manually", fontSize = 16.sp, color = palette.textPrimary) }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualLocationInputStep(accentColor: Color, palette: com.niagaraclone.launcher.ui.theme.LauncherPalette, onCitySelected: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(text = "Fixed location", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Your city or address", color = palette.textSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, unfocusedBorderColor = palette.borderColor, focusedTextColor = palette.textPrimary, unfocusedTextColor = palette.textPrimary, focusedContainerColor = palette.cardBackground, unfocusedContainerColor = palette.cardBackground)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (query.isNotBlank()) {
            Surface(shape = RoundedCornerShape(12.dp), color = palette.cardBackground, border = androidx.compose.foundation.BorderStroke(1.dp, palette.borderColor), modifier = Modifier.fillMaxWidth().clickable { onCitySelected(query) }.padding(vertical = 4.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "$query, India", color = palette.textPrimary, fontSize = 16.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ManualLocationConfirmStep(city: String, accentColor: Color, palette: com.niagaraclone.launcher.ui.theme.LauncherPalette, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Fixed location", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "$city, India", fontSize = 16.sp, color = palette.textSecondary)
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(52.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)) {
            Text(text = "Okay", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(text = "Cancel", fontSize = 16.sp, color = palette.textPrimary) }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TempUnitsStep(selectedUnit: String, accentColor: Color, palette: com.niagaraclone.launcher.ui.theme.LauncherPalette, onSelectUnit: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(text = "Temperature units", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth().clickable { onSelectUnit("CELSIUS") }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Celsius", fontSize = 16.sp, color = palette.textPrimary, modifier = Modifier.weight(1f))
            RadioButton(selected = (selectedUnit == "CELSIUS"), onClick = { onSelectUnit("CELSIUS") }, colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = palette.textSecondary))
        }

        Row(modifier = Modifier.fillMaxWidth().clickable { onSelectUnit("FAHRENHEIT") }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Fahrenheit", fontSize = 16.sp, color = palette.textPrimary, modifier = Modifier.weight(1f))
            RadioButton(selected = (selectedUnit == "FAHRENHEIT"), onClick = { onSelectUnit("FAHRENHEIT") }, colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = palette.textSecondary))
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
