package io.github.romanfabio.automations.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextRunCalculatorTest {
    private val rome = ZoneId.of("Europe/Rome")

    private fun at(date: String, hour: Int, minute: Int = 0) =
        ZonedDateTime.of(LocalDate.parse(date).atTime(hour, minute), rome)

    private fun only(vararg days: DayOfWeek) = days.fold(0) { mask, d -> Weekdays.toggle(mask, d) }

    @Test
    fun laterToday() {
        val next = NextRunCalculator.next(23, 0, Weekdays.ALL, at("2026-10-29", 10))
        assertEquals(at("2026-10-29", 23), next)
    }

    @Test
    fun timeAlreadyPassedMovesToTomorrow() {
        val next = NextRunCalculator.next(8, 0, Weekdays.ALL, at("2026-10-29", 10))
        assertEquals(at("2026-10-30", 8), next)
    }

    @Test
    fun exactlyNowIsNotNext() {
        val next = NextRunCalculator.next(10, 0, Weekdays.ALL, at("2026-10-29", 10))
        assertEquals(at("2026-10-30", 10), next)
    }

    @Test
    fun skipsUnselectedDays() {
        // 2026-10-30 is a Friday; Mon-Fri at 08:00 already passed -> next Monday.
        assertEquals(DayOfWeek.FRIDAY, LocalDate.parse("2026-10-30").dayOfWeek)
        val next = NextRunCalculator.next(8, 0, Weekdays.WORKDAYS, at("2026-10-30", 10))
        assertEquals(at("2026-11-02", 8), next)
    }

    @Test
    fun singleDayAlreadyPassedWaitsAWeek() {
        val thursday = only(DayOfWeek.THURSDAY)
        assertEquals(DayOfWeek.THURSDAY, LocalDate.parse("2026-10-29").dayOfWeek)
        val next = NextRunCalculator.next(8, 0, thursday, at("2026-10-29", 10))
        assertEquals(at("2026-11-05", 8), next)
    }

    @Test
    fun noDaysSelectedIsNull() {
        assertNull(NextRunCalculator.next(8, 0, 0, at("2026-10-29", 10)))
    }

    @Test
    fun timeInsideSpringForwardGapStillRuns() {
        // 2026-03-29 02:30 doesn't exist in Rome (clocks jump 02:00 -> 03:00).
        val next = NextRunCalculator.next(2, 30, Weekdays.ALL, at("2026-03-28", 23))!!
        assertEquals(LocalDateTime.parse("2026-03-29T03:30"), next.toLocalDateTime())
    }

    @Test
    fun acrossFallBackKeepsLocalTime() {
        // DST ends 2026-10-25; a 23:00 run on the 24th must be followed by 23:00 local on the 25th.
        val next = NextRunCalculator.next(23, 0, Weekdays.ALL, at("2026-10-24", 23, 30))!!
        assertEquals(LocalDateTime.parse("2026-10-25T23:00"), next.toLocalDateTime())
    }
}
