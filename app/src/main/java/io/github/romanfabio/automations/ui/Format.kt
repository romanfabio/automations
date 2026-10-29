package io.github.romanfabio.automations.ui

import io.github.romanfabio.automations.data.RunStatus
import io.github.romanfabio.automations.schedule.Weekdays
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

fun formatScheduleSummary(hour: Int, minute: Int, daysMask: Int): String =
    "${formatTime(hour, minute)} · ${Weekdays.describe(daysMask)}"

/** "Today 23:00", "Tomorrow 23:00", or "Mon 23:00". */
fun formatRelative(moment: ZonedDateTime, today: LocalDate = LocalDate.now(moment.zone)): String {
    val day = moment.toLocalDate()
    val label = when (day) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }
    return "$label ${moment.format(timeFormat)}"
}

fun formatTimestamp(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    formatRelative(Instant.ofEpochMilli(epochMillis).atZone(zone))

fun RunStatus.label(): String = when (this) {
    RunStatus.SUCCESS -> "Done"
    RunStatus.SKIPPED -> "Skipped"
    RunStatus.FAILED -> "Failed"
}
