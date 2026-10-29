package io.github.romanfabio.automations

import android.app.Application
import io.github.romanfabio.automations.automation.AutomationManager
import io.github.romanfabio.automations.automation.AutomationRunner
import io.github.romanfabio.automations.data.AppDatabase
import io.github.romanfabio.automations.data.AutomationRepository
import io.github.romanfabio.automations.schedule.AlarmScheduler
import io.github.romanfabio.automations.schedule.ScheduleStore

/**
 * Manual service locator: the app is small enough that a DI framework would be overkill.
 * Everything is lazy because receivers can start this process before the first unlock, when
 * Room's storage isn't readable yet; only [scheduleStore] and [scheduler] are safe to use then.
 */
class AutomationsApplication : Application() {
    val scheduleStore by lazy { ScheduleStore(this) }
    val scheduler by lazy { AlarmScheduler(this, scheduleStore) }
    val repository by lazy { AutomationRepository(AppDatabase.create(this)) }
    val runner by lazy { AutomationRunner(this, repository, scheduleStore) }
    val manager by lazy { AutomationManager(repository, scheduler, scheduleStore) }
}
