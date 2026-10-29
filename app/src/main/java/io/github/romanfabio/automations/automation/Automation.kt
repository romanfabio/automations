package io.github.romanfabio.automations.automation

import android.content.Context
import android.content.Intent
import io.github.romanfabio.automations.data.AutomationConfig
import io.github.romanfabio.automations.schedule.Weekdays

sealed interface RunResult {
    val message: String

    data class Success(override val message: String) : RunResult
    data class Skipped(override val message: String) : RunResult
    data class Failed(override val message: String) : RunResult
}

/** Something the user must set up on the phone before the automation can work. */
class Requirement(
    val title: String,
    val howToFix: String,
    /** System screen where the user can fix it, if there is one. */
    val fixIntent: Intent? = null,
    val isMet: (Context) -> Boolean,
)

/** Default time/days for a scheduled automation. */
data class Schedule(val hour: Int, val minute: Int, val daysMask: Int = Weekdays.ALL)

/** A predefined automation. Add new ones to [AutomationRegistry]. */
interface Automation {
    /** Stable key used for storage; never change it once shipped. */
    val id: String
    val title: String
    val description: String

    /** Null for automations that can only be launched manually. */
    val defaultSchedule: Schedule?

    fun requirements(): List<Requirement>

    suspend fun run(context: Context): RunResult
}

val Automation.isScheduled: Boolean get() = defaultSchedule != null

/** Automations start disabled so that nothing runs before the user opts in. */
fun Automation.defaultConfig(): AutomationConfig {
    val schedule = defaultSchedule ?: Schedule(0, 0)
    return AutomationConfig(id, enabled = false, schedule.hour, schedule.minute, schedule.daysMask)
}
