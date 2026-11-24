package com.anubad.offline.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

/** yyyyMMdd of a timestamp in the phone's time zone. */
fun localDayKey(millis: Long): String =
    SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(millis))

/** yyyyMMdd of a date-picker value (the picker returns UTC midnight). */
fun utcDayKey(utcMillis: Long): String =
    SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = UTC }.format(Date(utcMillis))

/** Start and end (exclusive) of the picked calendar day, in local time. */
fun localDayRange(utcMillis: Long): Pair<Long, Long> {
    val utc = Calendar.getInstance(UTC).apply { timeInMillis = utcMillis }
    val local = Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    }
    val start = local.timeInMillis
    local.add(Calendar.DAY_OF_MONTH, 1)
    return start to local.timeInMillis
}

fun formatPickedDay(utcMillis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).apply { timeZone = UTC }.format(Date(utcMillis))

fun formatTime(millis: Long): String =
    SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(millis))

fun formatCsvTime(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(millis))
