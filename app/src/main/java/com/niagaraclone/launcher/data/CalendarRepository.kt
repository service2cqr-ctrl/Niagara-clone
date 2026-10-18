package com.niagaraclone.launcher.data

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CalendarEventModel(
    val id: Long = 0L,
    val title: String = "",
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long = 0L,
    val isAllDay: Boolean = false,
    val colorArgb: Int? = null,
    val location: String = ""
)

class CalendarRepository(private val context: Context) {

    suspend fun getUpcomingEvents(daysAhead: Int = 14): List<CalendarEventModel> = withContext(Dispatchers.IO) {
        val events = mutableListOf<CalendarEventModel>()
        val nowMillis = System.currentTimeMillis()
        val endMillis = nowMillis + (daysAhead * 24 * 60 * 60 * 1000L)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.EVENT_LOCATION
        )

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, nowMillis)
        ContentUris.appendId(builder, endMillis)

        try {
            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                "${CalendarContract.Instances.END} >= ?",
                arrayOf(nowMillis.toString()),
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIdx = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = it.getColumnIndex(CalendarContract.Instances.END)
                val allDayIdx = it.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val colorIdx = it.getColumnIndex(CalendarContract.Instances.DISPLAY_COLOR)
                val locIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)

                while (it.moveToNext() && events.size < 35) {
                    if (idIdx != -1 && beginIdx != -1) {
                        val rawColor = if (colorIdx != -1 && !it.isNull(colorIdx)) it.getInt(colorIdx) else null
                        val rawLocation = if (locIdx != -1 && !it.isNull(locIdx)) it.getString(locIdx).orEmpty() else ""

                        events.add(
                            CalendarEventModel(
                                id = it.getLong(idIdx),
                                title = if (titleIdx != -1) it.getString(titleIdx)?.takeIf { t -> t.isNotBlank() } ?: "Untitled event" else "Untitled event",
                                startTimeMillis = it.getLong(beginIdx),
                                endTimeMillis = if (endIdx != -1) it.getLong(endIdx) else it.getLong(beginIdx),
                                isAllDay = if (allDayIdx != -1) it.getInt(allDayIdx) == 1 else false,
                                colorArgb = rawColor,
                                location = rawLocation
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        events
    }
}
