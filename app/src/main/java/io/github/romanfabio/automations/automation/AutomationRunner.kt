package io.github.romanfabio.automations.automation

import android.content.Context
import android.os.UserManager
import io.github.romanfabio.automations.data.AutomationRepository
import io.github.romanfabio.automations.data.RunLog
import io.github.romanfabio.automations.data.RunStatus
import io.github.romanfabio.automations.data.RunTrigger
import io.github.romanfabio.automations.schedule.ScheduleStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Runs an automation and records the outcome. Runs are serialized so two sessions never overlap. */
class AutomationRunner(
    private val context: Context,
    private val repository: AutomationRepository,
    private val store: ScheduleStore,
) {
    private val mutex = Mutex()

    suspend fun run(automation: Automation, trigger: RunTrigger): RunResult = mutex.withLock {
        val result = try {
            automation.run(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RunResult.Failed(e.message ?: e::class.java.simpleName)
        }
        record(
            RunLog(
                automationId = automation.id,
                timestamp = System.currentTimeMillis(),
                trigger = trigger,
                status = when (result) {
                    is RunResult.Success -> RunStatus.SUCCESS
                    is RunResult.Skipped -> RunStatus.SKIPPED
                    is RunResult.Failed -> RunStatus.FAILED
                },
                message = result.message,
            ),
        )
        result
    }

    /** Before the first unlock after a reboot Room can't be opened, so the entry is queued for later. */
    private suspend fun record(entry: RunLog) {
        if (context.getSystemService(UserManager::class.java).isUserUnlocked) {
            repository.log(entry)
        } else {
            store.addPendingLog(entry)
        }
    }
}
