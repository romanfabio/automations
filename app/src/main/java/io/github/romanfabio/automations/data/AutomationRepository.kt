package io.github.romanfabio.automations.data

import io.github.romanfabio.automations.automation.Automation
import io.github.romanfabio.automations.automation.defaultConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AutomationRepository(private val db: AppDatabase) {
    private val configDao = db.configDao()
    private val logDao = db.runLogDao()

    /** Stored configs by automation id. Automations never edited have no row (use [defaultConfig]). */
    val configs: Flow<Map<String, AutomationConfig>> =
        configDao.observeAll().map { rows -> rows.associateBy { it.id } }

    val latestRuns: Flow<Map<String, RunLog>> =
        logDao.observeLatestPerAutomation().map { rows -> rows.associateBy { it.automationId } }

    fun history(automationId: String): Flow<List<RunLog>> = logDao.observeHistory(automationId, HISTORY_LIMIT)

    suspend fun configFor(automation: Automation): AutomationConfig =
        configDao.get(automation.id) ?: automation.defaultConfig()

    suspend fun update(automation: Automation, change: (AutomationConfig) -> AutomationConfig) {
        configDao.upsert(change(configFor(automation)))
    }

    suspend fun log(entry: RunLog) {
        logDao.insert(entry)
        logDao.prune(LOG_KEEP)
    }

    private companion object {
        const val HISTORY_LIMIT = 50
        const val LOG_KEEP = 200
    }
}
