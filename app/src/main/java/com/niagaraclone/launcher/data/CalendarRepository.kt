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
    val isAllDay: Boolean = false
)

class CalendarRepository(private val context: Context) {

    suspend fun getUpcomingEvents(daysAhead: Int = 2): List<CalendarEventModel> = withContext(Dispatchers.IO) {
        val events = mutableListOf<CalendarEventModel>()
        val startMillis = System.currentTimeMillis()
        val endMillis = startMillis + (daysAhead * 24 * 60 * 60 * 1000L)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        try {
            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val titleIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDayIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

                while (it.moveToNext()) {
                    events.add(
                        CalendarEventModel(
                            id = it.getLong(idIdx),
                            title = it.getString(titleIdx) ?: "Untitled Event",
                            startTimeMillis = it.getLong(beginIdx),
                            endTimeMillis = it.getLong(endIdx),
                            isAllDay = it.getInt(allDayIdx) == 1
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        events
    }
}
