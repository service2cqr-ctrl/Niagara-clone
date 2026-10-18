package com.niagaraclone.launcher.ui.components

import android.content.ContentUris
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.CalendarEventModel
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.theme.LocalLauncherPalette
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class HourlyForecast(
    val time: String,
    val icon: String,
    val tempStr: String,
    val popStr: String
)

private data class DayAgendaSection(
    val dayKey: Int,
    val dateHeader: String,
    val relativeLabel: String,
    val events: List<CalendarEventModel>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarAgendaSheet(
    viewModel: LauncherViewModel,
    events: List<CalendarEventModel>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalLauncherPalette.current

    val weatherEnabled by viewModel.weatherEnabled.collectAsState()
    val liveWeather by viewModel.liveWeather.collectAsState()
    val tempUnit by viewModel.weatherTempUnit.collectAsState()
    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val accentColor = Color(accentColorArgb)

    LaunchedEffect(Unit) {
        viewModel.loadCalendarEvents()
    }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    val baseTempC = liveWeather?.temperatureCelsius ?: 28f
    val formatTemp: (Float) -> String = { tempC ->
        val value = if (tempUnit == "CELSIUS") tempC.roundToInt() else (tempC * 9f / 5f + 32f).roundToInt()
        "$value°"
    }
    val baseIcon = liveWeather?.conditionIcon ?: "☀️"

    val hourlyList = remember(liveWeather, tempUnit) {
        val hourFormatter = SimpleDateFormat("h a", Locale.getDefault())
        val cal = Calendar.getInstance()
        val tempOffsets = listOf(0f, 0.5f, 0f, -1f, -1.8f, -2.5f)

        tempOffsets.mapIndexed { idx, delta ->
            val label = if (idx == 0) {
                "now"
            } else {
                cal.add(Calendar.HOUR_OF_DAY, 1)
                hourFormatter.format(cal.time).lowercase(Locale.getDefault())
            }
            val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
            val iconForHour = when {
                idx == 0 -> baseIcon
                hourOfDay in 19..23 || hourOfDay in 0..5 -> "🌙"
                idx >= 3 && baseIcon == "☀️" -> "🌤️"
                else -> baseIcon
            }
            HourlyForecast(
                time = label,
                icon = iconForHour,
                tempStr = formatTemp(baseTempC + delta),
                popStr = ""
            )
        }
    }

    val daySections = remember(events) {
        val dayHeaderFormatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val todayCal = Calendar.getInstance()
        val todayKey = todayCal.get(Calendar.YEAR) * 1000 + todayCal.get(Calendar.DAY_OF_YEAR)

        val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowKey = tomorrowCal.get(Calendar.YEAR) * 1000 + tomorrowCal.get(Calendar.DAY_OF_YEAR)

        val groupedMap = linkedMapOf<Int, MutableList<CalendarEventModel>>()
        val dateForDayKey = mutableMapOf<Int, Date>()

        groupedMap[todayKey] = mutableListOf()
        dateForDayKey[todayKey] = todayCal.time

        val eventCal = Calendar.getInstance()
        events.forEach { event ->
            eventCal.timeInMillis = event.startTimeMillis
            val key = eventCal.get(Calendar.YEAR) * 1000 + eventCal.get(Calendar.DAY_OF_YEAR)
            val effectiveKey = if (key < todayKey) todayKey else key
            val list = groupedMap.getOrPut(effectiveKey) { mutableListOf() }
            list.add(event)
            if (!dateForDayKey.containsKey(effectiveKey)) {
                dateForDayKey[effectiveKey] = Date(event.startTimeMillis)
            }
        }

        groupedMap.entries.map { (key, dayEvents) ->
            val dateObj = dateForDayKey[key] ?: todayCal.time
            val rel = when (key) {
                todayKey -> "today"
                tomorrowKey -> "tomorrow"
                else -> ""
            }
            DayAgendaSection(
                dayKey = key,
                dateHeader = dayHeaderFormatter.format(dateObj),
                relativeLabel = rel,
                events = dayEvents
            )
        }
    }

    val openCalendarApp = {
        try {
            val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
            ContentUris.appendId(builder, System.currentTimeMillis())
            val intent = Intent(Intent.ACTION_VIEW).setData(builder.build()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            onDismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.sheetBackground,
        contentColor = palette.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // --- COMPACT HEADER ROW ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Your agenda",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }

                IconButton(
                    onClick = openCalendarApp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = "Open Calendar App",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // --- SLIM HOURLY WEATHER STRIP ---
            if (weatherEnabled) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.cardBackground,
                    border = if (palette.isGlass) androidx.compose.foundation.BorderStroke(1.dp, palette.borderColor) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(hourlyList) { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.time,
                                    color = palette.textSecondary,
                                    fontSize = 11.5.sp
                                )
                                Text(text = item.icon, fontSize = 14.sp)
                                Text(
                                    text = item.tempStr,
                                    color = palette.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // --- AGENDA TIMELINE LIST ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                daySections.forEach { section ->
                    item(key = "day_header_${section.dayKey}") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = section.dateHeader,
                                    color = palette.textPrimary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (section.relativeLabel.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = accentColor.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = section.relativeLabel,
                                            color = accentColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            if (section.relativeLabel == "today" && weatherEnabled) {
                                Text(
                                    text = "$baseIcon ${formatTemp(baseTempC)}",
                                    color = palette.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (section.events.isEmpty() && section.relativeLabel == "today") {
                        item(key = "empty_today") {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.cardBackground,
                                border = if (palette.isGlass) androidx.compose.foundation.BorderStroke(1.dp, palette.borderColor) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.EventAvailable,
                                        contentDescription = null,
                                        tint = palette.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (events.isEmpty()) "No upcoming events scheduled" else "Nothing else scheduled for today",
                                        color = palette.textSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = section.events,
                            key = { "evt_${section.dayKey}_${it.id}_${it.startTimeMillis}" }
                        ) { event ->
                            val indicatorColor = event.colorArgb?.let { Color(it) } ?: accentColor
                            val timeText = if (event.isAllDay) {
                                "All day"
                            } else {
                                "${timeFormat.format(Date(event.startTimeMillis))} – ${timeFormat.format(Date(event.endTimeMillis))}"
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.cardBackground,
                                border = if (palette.isGlass) androidx.compose.foundation.BorderStroke(1.dp, palette.borderColor) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.openCalendarEvent(context, event.id)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(28.dp)
                                            .background(indicatorColor, RoundedCornerShape(1.5.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = event.title,
                                            color = palette.textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        val subLine = if (event.location.isNotBlank()) {
                                            "$timeText · ${event.location}"
                                        } else {
                                            timeText
                                        }
                                        Text(
                                            text = subLine,
                                            color = palette.textSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- COMPACT NEW EVENT BUTTON ---
            OutlinedButton(
                onClick = {
                    viewModel.launchCalendarNewEvent(context)
                    onDismiss()
                },
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.textPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 12.dp)
                    .height(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "New event",
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
