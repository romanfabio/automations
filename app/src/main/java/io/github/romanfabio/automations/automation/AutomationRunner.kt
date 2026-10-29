package io.github.romanfabio.automations.automation

import android.content.Context
import io.github.romanfabio.automations.data.AutomationRepository
import io.github.romanfabio.automations.data.RunLog
import io.github.romanfabio.automations.data.RunStatus
import io.github.romanfabio.automations.data.RunTrigger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Runs an automation and records the outcome. Runs are serialized so two sessions never overlap. */
class AutomationRunner(
    private val context: Context,
    private val repository: AutomationRepository,
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
        repository.log(
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
}
