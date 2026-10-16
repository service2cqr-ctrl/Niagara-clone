package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.CalendarEventModel
import com.niagaraclone.launcher.ui.LauncherViewModel
import java.text.SimpleDateFormat
import java.util.*

data class HourlyForecast(
    val time: String,
    val icon: String,
    val tempStr: String,
    val popStr: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarAgendaSheet(
    viewModel: LauncherViewModel,
    events: List<CalendarEventModel>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val currentDateStr = remember { SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date()) }

    val liveWeather by viewModel.liveWeather.collectAsState()
    val tempUnit by viewModel.weatherTempUnit.collectAsState()
    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val accentColor = Color(accentColorArgb)

    val currentTempNum = liveWeather?.temperatureCelsius ?: 28f
    val currentTempFormatted = if (tempUnit == "CELSIUS") "${currentTempNum.toInt()}°" else "${(currentTempNum * 9 / 5 + 32).toInt()}°"
    val weatherIcon = liveWeather?.conditionIcon ?: "☀️"

    // Dynamic hourly forecast building around current conditions
    val hourlyList = remember(liveWeather, tempUnit) {
        listOf(
            HourlyForecast("Now", weatherIcon, currentTempFormatted, ""),
            HourlyForecast("+2h", weatherIcon, currentTempFormatted, "20%"),
            HourlyForecast("+4h", "☁️", currentTempFormatted, ""),
            HourlyForecast("+6h", "🌧️", currentTempFormatted, "60%"),
            HourlyForecast("+8h", "🌙", currentTempFormatted, "")
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141414),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Your agenda",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Hourly Weather Carousel
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                items(hourlyList) { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(52.dp)
                    ) {
                        Text(text = item.time, color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = item.icon, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = item.tempStr, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        if (item.popStr.isNotEmpty()) {
                            Text(text = item.popStr, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // + New Event Button
            Row(
                modifier = Modifier
                    .clickable {
                        viewModel.launchCalendarNewEvent(context)
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "New event", color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Events List
            if (events.isEmpty()) {
                Text(
                    text = "No upcoming events scheduled.",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    item {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "$currentDateStr  today", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(text = "$currentTempFormatted • live update", color = Color.Gray, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    items(events) { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.openCalendarEvent(context, event.id)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(32.dp)
                                    .background(accentColor, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = event.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                val timeText = if (event.isAllDay) "All Day"
                                else "${timeFormat.format(Date(event.startTimeMillis))} - ${timeFormat.format(Date(event.endTimeMillis))}"
                                Text(text = timeText, color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
