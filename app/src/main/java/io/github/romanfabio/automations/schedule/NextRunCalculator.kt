package io.github.romanfabio.automations.schedule

import java.time.LocalTime
import java.time.ZonedDateTime

object NextRunCalculator {
    /**
     * First moment strictly after [now] that falls on one of the [daysMask] weekdays at [hour]:[minute]
     * in [now]'s zone, or null if no day is selected. A time that doesn't exist because of a DST gap
     * moves forward by the gap, so the run still happens that day.
     */
    fun next(hour: Int, minute: Int, daysMask: Int, now: ZonedDateTime): ZonedDateTime? {
        if (daysMask and Weekdays.ALL == 0) return null
        val time = LocalTime.of(hour, minute)
        for (offset in 0L..7L) {
            val date = now.toLocalDate().plusDays(offset)
            if (!Weekdays.has(daysMask, date.dayOfWeek)) continue
            val candidate = ZonedDateTime.of(date, time, now.zone)
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }
}
