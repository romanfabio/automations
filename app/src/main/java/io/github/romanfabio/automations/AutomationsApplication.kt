package io.github.romanfabio.automations

import android.app.Application
import io.github.romanfabio.automations.automation.AutomationRunner
import io.github.romanfabio.automations.data.AppDatabase
import io.github.romanfabio.automations.data.AutomationRepository

/** Manual service locator: the app is small enough that a DI framework would be overkill. */
class AutomationsApplication : Application() {
    val repository by lazy { AutomationRepository(AppDatabase.create(this)) }
    val runner by lazy { AutomationRunner(this, repository) }
}
