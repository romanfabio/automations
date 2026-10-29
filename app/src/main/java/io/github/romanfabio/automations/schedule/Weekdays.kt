package io.github.romanfabio.automations.schedule

import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Weekday bitmask: bit 0 = Monday ... bit 6 = Sunday. */
object Weekdays {
    const val ALL = 0b1111111
    const val WORKDAYS = 0b0011111

    private fun bit(day: DayOfWeek) = 1 shl (day.value - 1)

    fun has(mask: Int, day: DayOfWeek): Boolean = mask and bit(day) != 0

    fun toggle(mask: Int, day: DayOfWeek): Int = mask xor bit(day)

    fun describe(mask: Int, locale: Locale = Locale.getDefault()): String = when (mask) {
        ALL -> "every day"
        WORKDAYS -> "weekdays"
        0 -> "no days"
        else -> DayOfWeek.entries.filter { has(mask, it) }
            .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
    }
}
