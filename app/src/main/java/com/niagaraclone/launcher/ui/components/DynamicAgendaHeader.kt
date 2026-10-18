package com.niagaraclone.launcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.CalendarEventModel
import com.niagaraclone.launcher.ui.theme.LocalLauncherPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DynamicAgendaHeader(
    clockStyle: String,
    timeHours: String,
    timeMinutes: String,
    currentDate: String,
    batteryStatus: String,
    showBattery: Boolean,
    weatherEnabled: Boolean,
    weatherTempDisplay: String,
    showCalendarPreview: Boolean,
    upcomingEvents: List<CalendarEventModel>,
    accentColor: Color,
    onDateClick: () -> Unit,
    onWeatherClick: () -> Unit,
    onEventClick: (Long) -> Unit
) {
    val palette = LocalLauncherPalette.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- CLOCK DISPLAY ---
        when (clockStyle) {
            "DIGITAL_MINIMAL" -> Text(
                text = "$timeHours:$timeMinutes",
                color = palette.textPrimary,
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold
            )
            "DIGITAL_HEADER" -> Column {
                Text(
                    text = currentDate.uppercase(),
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "$timeHours:$timeMinutes",
                    color = palette.textPrimary,
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeHours,
                    color = accentColor,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timeMinutes,
                    color = palette.textPrimary,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- UNIFIED STATUS BAR ROW ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = currentDate.ifEmpty { "Wed 5 Aug" },
                color = palette.textPrimary.copy(alpha = 0.9f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onDateClick)
            )

            if (showBattery) {
                Text(text = "•", color = palette.textSecondary, fontSize = 12.sp)
                Text(
                    text = batteryStatus,
                    color = palette.textPrimary.copy(alpha = 0.9f),
                    fontSize = 15.sp
                )
            }

            if (weatherEnabled) {
                Text(text = "•", color = palette.textSecondary, fontSize = 12.sp)
                Text(
                    text = weatherTempDisplay,
                    color = palette.textPrimary.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    modifier = Modifier.clickable(onClick = onWeatherClick)
                )
            }
        }

        // --- INLINE CALENDAR AGENDA PREVIEW ---
        AnimatedVisibility(visible = showCalendarPreview && upcomingEvents.isNotEmpty()) {
            val nextEvent = upcomingEvents.firstOrNull()
            if (nextEvent != null) {
                val eventTimeStr = if (nextEvent.isAllDay) {
                    "All day"
                } else {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(nextEvent.startTimeMillis))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .clickable { onEventClick(nextEvent.id) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(accentColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "$eventTimeStr • ${nextEvent.title}",
                        color = palette.textPrimary.copy(alpha = 0.88f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
