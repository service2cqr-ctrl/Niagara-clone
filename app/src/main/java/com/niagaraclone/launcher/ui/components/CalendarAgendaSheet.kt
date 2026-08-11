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
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

    // Mock/Live hourly forecast array matching Video #2
    val hourlyList = listOf(
        HourlyForecast("now", "💧", "29°", ""),
        HourlyForecast("2 pm", "💧", "28°", "100%"),
        HourlyForecast("3 pm", "💧", "28°", "100%"),
        HourlyForecast("4 pm", "🌧️", "27°", "100%"),
        HourlyForecast("5 pm", "🌦️", "26°", "80%"),
        HourlyForecast("6 pm", "☁️", "25°", ""),
        HourlyForecast("7 pm", "🌙", "24°", "")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
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
                        modifier = Modifier.width(48.dp)
                    ) {
                        Text(text = item.time, color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = item.icon, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = item.tempStr, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        if (item.popStr.isNotEmpty()) {
                            Text(text = item.popStr, color = Color(0xFF4DD0E1), fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF4DD0E1), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "New event", color = Color(0xFF4DD0E1), fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                                Text(text = "Tue 11 Aug  today", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(text = "26° / 23° 40% • light rain", color = Color.Gray, fontSize = 12.sp)
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
                                    .background(Color(0xFF4DD0E1), RoundedCornerShape(2.dp))
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
