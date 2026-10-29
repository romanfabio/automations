package io.github.romanfabio.automations.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.romanfabio.automations.AutomationsApplication
import io.github.romanfabio.automations.automation.Automation
import io.github.romanfabio.automations.automation.AutomationRegistry
import io.github.romanfabio.automations.automation.Requirement
import io.github.romanfabio.automations.automation.defaultConfig
import io.github.romanfabio.automations.automation.isScheduled
import io.github.romanfabio.automations.data.AutomationConfig
import io.github.romanfabio.automations.data.RunLog
import io.github.romanfabio.automations.data.RunTrigger
import io.github.romanfabio.automations.schedule.NextRunCalculator
import io.github.romanfabio.automations.setup.SetupChecker
import io.github.romanfabio.automations.setup.SetupItem
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AutomationItem(
    val automation: Automation,
    val config: AutomationConfig,
    val lastRun: RunLog?,
    /** Null when disabled, manual-only, or no weekday is selected. */
    val nextRun: ZonedDateTime?,
    val unmetRequirements: List<Requirement>,
)

class AutomationsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AutomationsApplication
    private val repository = app.repository

    /** Bumped on resume: requirements (e.g. the assistant role) change outside the app. */
    private val refreshTick = MutableStateFlow(0)

    val items: StateFlow<List<AutomationItem>> =
        combine(repository.configs, repository.latestRuns, refreshTick) { configs, runs, _ ->
            val now = ZonedDateTime.now()
            AutomationRegistry.all.map { automation ->
                val config = configs[automation.id] ?: automation.defaultConfig()
                AutomationItem(
                    automation = automation,
                    config = config,
                    lastRun = runs[automation.id],
                    nextRun = if (automation.isScheduled && config.enabled) {
                        NextRunCalculator.next(config.hour, config.minute, config.daysMask, now)
                    } else {
                        null
                    },
                    unmetRequirements = automation.requirements().filterNot { it.isMet(app) },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val setupChecker = SetupChecker(app)
    private val _setup = MutableStateFlow<List<SetupItem>>(emptyList())
    val setup: StateFlow<List<SetupItem>> = _setup

    init {
        refreshSetup()
        // Self-heal: re-arm alarms that the system may have dropped (e.g. after a force stop).
        viewModelScope.launch { app.manager.resyncAll() }
    }

    fun markSetupDone(item: SetupItem) {
        setupChecker.markManualDone(item.id)
        refreshSetup()
    }

    private fun refreshSetup() {
        _setup.value = setupChecker.items(AutomationRegistry.all)
    }

    fun history(automationId: String): Flow<List<RunLog>> = repository.history(automationId)

    fun refresh() {
        refreshTick.value++
        refreshSetup()
    }

    fun setEnabled(automation: Automation, enabled: Boolean) = update(automation) { it.copy(enabled = enabled) }

    fun setTime(automation: Automation, hour: Int, minute: Int) =
        update(automation) { it.copy(hour = hour, minute = minute) }

    fun setDays(automation: Automation, daysMask: Int) = update(automation) { it.copy(daysMask = daysMask) }

    fun runNow(automation: Automation) {
        viewModelScope.launch { app.runner.run(automation, RunTrigger.MANUAL) }
    }

    private fun update(automation: Automation, change: (AutomationConfig) -> AutomationConfig) {
        viewModelScope.launch { app.manager.update(automation, change) }
    }
}
