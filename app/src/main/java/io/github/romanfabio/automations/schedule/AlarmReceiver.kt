package io.github.romanfabio.automations.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.romanfabio.automations.AutomationsApplication
import io.github.romanfabio.automations.automation.AutomationRegistry
import io.github.romanfabio.automations.automation.RunResult
import io.github.romanfabio.automations.data.RunTrigger
import io.github.romanfabio.automations.notify.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fired by the alarm of one automation. Direct-boot aware: it can run before the first unlock. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val app = context.applicationContext as AutomationsApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                handleFire(app, id)
            } catch (e: Exception) {
                Log.e(TAG, "alarm for $id failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handleFire(app: AutomationsApplication, id: String) {
        val automation = AutomationRegistry.byId(id) ?: return
        val config = app.scheduleStore.get(id)
        if (config == null || !config.enabled) return // disabled after the alarm was queued

        // Arm tomorrow's alarm first, so a crash or slow run can't lose the schedule.
        app.scheduler.arm(config)

        val result = app.runner.run(automation, RunTrigger.SCHEDULED)
        Log.i(TAG, "$id -> $result")
        if (result is RunResult.Failed) Notifier.notifyFailure(app, automation, result.message)
    }

    companion object {
        private const val TAG = "AutoRun"
        const val ACTION_FIRE = "io.github.romanfabio.automations.FIRE"
        const val EXTRA_ID = "id"
    }
}
