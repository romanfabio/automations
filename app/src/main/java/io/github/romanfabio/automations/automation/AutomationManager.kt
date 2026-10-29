package io.github.romanfabio.automations.automation

import io.github.romanfabio.automations.data.AutomationConfig
import io.github.romanfabio.automations.data.AutomationRepository
import io.github.romanfabio.automations.schedule.AlarmScheduler
import io.github.romanfabio.automations.schedule.ScheduleStore

/** Single entry point for changing an automation's config so Room, the mirror and the alarm never diverge. */
class AutomationManager(
    private val repository: AutomationRepository,
    private val scheduler: AlarmScheduler,
    private val store: ScheduleStore,
) {
    suspend fun update(automation: Automation, change: (AutomationConfig) -> AutomationConfig) {
        repository.update(automation, change)
        if (automation.isScheduled) scheduler.apply(repository.configFor(automation))
    }

    /** Re-syncs the mirror and alarms from Room and imports log entries queued while Room was locked. */
    suspend fun resyncAll() {
        store.takePendingLogs().forEach { repository.log(it) }
        AutomationRegistry.all.filter { it.isScheduled }.forEach { scheduler.apply(repository.configFor(it)) }
    }
}
