package io.github.romanfabio.automations.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.romanfabio.automations.AutomationsApplication
import io.github.romanfabio.automations.automation.AutomationRegistry
import io.github.romanfabio.automations.data.RunTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug builds only; drives the real code paths from adb.
 *
 * Change config (omitted extras keep their current value):
 *   adb shell am broadcast -n io.github.romanfabio.automations/.debug.DebugReceiver -a CONFIG \
 *       --es id airplane_on --ez enabled true --ei hour 20 --ei minute 30 --ei days 127
 * Run now, as if the Run now button was tapped (works with the app in the background):
 *   adb shell am broadcast -n io.github.romanfabio.automations/.debug.DebugReceiver -a RUN --es id airplane_on
 */
class DebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as AutomationsApplication
        val automation = AutomationRegistry.byId(intent.getStringExtra("id") ?: return) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    "CONFIG" -> {
                        app.manager.update(automation) { c ->
                            c.copy(
                                enabled = intent.getBooleanExtra("enabled", c.enabled),
                                hour = intent.getIntExtra("hour", c.hour),
                                minute = intent.getIntExtra("minute", c.minute),
                                daysMask = intent.getIntExtra("days", c.daysMask),
                            )
                        }
                        Log.i("AutoSched", "debug config applied: ${app.repository.configFor(automation)}")
                    }
                    "RUN" -> Log.i("AutoRun", "debug run: ${app.runner.run(automation, RunTrigger.MANUAL)}")
                }
            } finally {
                pending.finish()
            }
        }
    }
}
