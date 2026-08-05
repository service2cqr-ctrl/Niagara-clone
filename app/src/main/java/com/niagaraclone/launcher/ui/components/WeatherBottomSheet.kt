package com.niagaraclone.launcher.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.ChevronRight
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

private enum class WeatherStep {
    MAIN,
    CHOOSE_LOCATION,
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
    var currentStep by remember { mutableStateOf(WeatherStep.MAIN) }

    val weatherEnabled by viewModel.weatherEnabled.collectAsState()
    val locationMode by viewModel.weatherLocationMode.collectAsState()
    val cityName by viewModel.weatherCityName.collectAsState()
    val tempUnit by viewModel.weatherTempUnit.collectAsState()

    var tempSearchCity by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141414),
        contentColor = Color.White,
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
                    tempDisplay = if (tempUnit == "CELSIUS") "28°" else "82°",
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
                    onSelectAuto = {
                        viewModel.setWeatherLocationMode("AUTO")
                        viewModel.setWeatherCityName("Vadodara")
                        currentStep = WeatherStep.MAIN
                    },
                    onSelectFixed = {
                        currentStep = WeatherStep.FIXED_LOCATION_PROMPT
                    }
                )

                WeatherStep.FIXED_LOCATION_PROMPT -> FixedLocationPromptStep(
                    onOkayAutoDetect = {
                        viewModel.setWeatherLocationMode("FIXED")
                        viewModel.setWeatherCityName("Vadodara")
                        currentStep = WeatherStep.MAIN
                    },
                    onEnterManually = {
                        currentStep = WeatherStep.MANUAL_LOCATION_INPUT
                    }
                )

                WeatherStep.MANUAL_LOCATION_INPUT -> ManualLocationInputStep(
                    onCitySelected = { city ->
                        tempSearchCity = city
                        currentStep = WeatherStep.MANUAL_LOCATION_CONFIRM
                    }
                )

                WeatherStep.MANUAL_LOCATION_CONFIRM -> ManualLocationConfirmStep(
                    city = tempSearchCity,
                    onConfirm = {
                        viewModel.setWeatherLocationMode("FIXED")
                        viewModel.setWeatherCityName(tempSearchCity)
                        currentStep = WeatherStep.MAIN
                    },
                    onCancel = {
                        currentStep = WeatherStep.MAIN
                    }
                )

                WeatherStep.TEMP_UNITS -> TempUnitsStep(
                    selectedUnit = tempUnit,
                    onSelectUnit = { unit ->
                        viewModel.setWeatherTempUnit(unit)
                        currentStep = WeatherStep.MAIN
                    }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// STEP 1: MAIN WEATHER PREVIEW SHEET
// -----------------------------------------------------------------------------
@Composable
private fun MainWeatherStep(
    locationText: String,
    tempUnitText: String,
    tempDisplay: String,
    onOpenLocation: () -> Unit,
    onOpenTempUnits: () -> Unit,
    onKeepEnabled: () -> Unit,
    onDisable: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Icon(imageVector = Icons.Default.WbSunny, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Weather", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // Dark Preview Container Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E1E),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "8:33", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Wed 5 Aug", fontSize = 16.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "☀️ $tempDisplay", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Location Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenLocation)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Location", fontSize = 16.sp, color = Color.White)
                Text(text = locationText, fontSize = 13.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }

        // Temperature Units Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenTempUnits)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Style, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Temperature units", fontSize = 16.sp, color = Color.White)
                Text(text = tempUnitText, fontSize = 13.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tapping the weather widget reveals hourly and daily forecast. Learn more",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            text = "Powered by OpenWeather",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Action Buttons
        Button(
            onClick = onKeepEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
            Text(text = "Keep enabled", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onDisable,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Disable", fontSize = 16.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// -----------------------------------------------------------------------------
// STEP 2: CHOOSE LOCATION STEP
// -----------------------------------------------------------------------------
@Composable
private fun ChooseLocationStep(
    selectedMode: String,
    onSelectAuto: () -> Unit,
    onSelectFixed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text = "Choose location", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We share this location with our weather provider to give you a regional forecast. We don't use it for anything else.",
            fontSize = 13.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelectAuto)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Current location", fontSize = 16.sp, color = Color.White)
                Text(text = "Updates automatically", fontSize = 13.sp, color = Color.Gray)
            }
            RadioButton(
                selected = (selectedMode == "AUTO"),
                onClick = onSelectAuto,
                colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.Gray)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelectFixed)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Fixed location", fontSize = 16.sp, color = Color.White)
            }
            RadioButton(
                selected = (selectedMode == "FIXED"),
                onClick = onSelectFixed,
                colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.Gray)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// -----------------------------------------------------------------------------
// STEP 3: FIXED LOCATION PROMPT STEP
// -----------------------------------------------------------------------------
@Composable
private fun FixedLocationPromptStep(
    onOkayAutoDetect: () -> Unit,
    onEnterManually: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text = "Fixed location", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Do you want us to detect your location automatically? This generally gives more accurate forecasts than entering it manually.",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onOkayAutoDetect,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
            Text(text = "Okay", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onEnterManually, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Enter manually", fontSize = 16.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// -----------------------------------------------------------------------------
// STEP 4: MANUAL LOCATION INPUT STEP
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualLocationInputStep(
    onCitySelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text = "Fixed location", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Your city or address", color = Color.Gray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4DD0E1),
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (query.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF222222),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCitySelected(query) }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF4DD0E1))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "$query, India", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// -----------------------------------------------------------------------------
// STEP 5: MANUAL LOCATION CONFIRM STEP
// -----------------------------------------------------------------------------
@Composable
private fun ManualLocationConfirmStep(
    city: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Fixed location", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "$city, India", fontSize = 16.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
            Text(text = "Okay", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Cancel", fontSize = 16.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// -----------------------------------------------------------------------------
// STEP 6: TEMPERATURE UNITS STEP
// -----------------------------------------------------------------------------
@Composable
private fun TempUnitsStep(
    selectedUnit: String,
    onSelectUnit: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text = "Temperature units", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectUnit("CELSIUS") }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Celsius", fontSize = 16.sp, color = Color.White, modifier = Modifier.weight(1f))
            RadioButton(
                selected = (selectedUnit == "CELSIUS"),
                onClick = { onSelectUnit("CELSIUS") },
                colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.Gray)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectUnit("FAHRENHEIT") }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Fahrenheit", fontSize = 16.sp, color = Color.White, modifier = Modifier.weight(1f))
            RadioButton(
                selected = (selectedUnit == "FAHRENHEIT"),
                onClick = { onSelectUnit("FAHRENHEIT") },
                colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.Gray)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
