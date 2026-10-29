package io.github.romanfabio.automations.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.romanfabio.automations.AutomationsApplication
import io.github.romanfabio.automations.automation.AutomationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * adb shell am broadcast -n io.github.romanfabio.automations/.debug.DebugReceiver -a CONFIG \
 *     --es id airplane_on --ez enabled true --ei hour 20 --ei minute 30 --ei days 127
 * Omitted extras keep their current value.
 */
class DebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "CONFIG") return
        val app = context.applicationContext as AutomationsApplication
        val automation = AutomationRegistry.byId(intent.getStringExtra("id") ?: return) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.manager.update(automation) { c ->
                    c.copy(
                        enabled = if (intent.hasExtra("enabled")) intent.getBooleanExtra("enabled", c.enabled) else c.enabled,
                        hour = if (intent.hasExtra("hour")) intent.getIntExtra("hour", c.hour) else c.hour,
                        minute = if (intent.hasExtra("minute")) intent.getIntExtra("minute", c.minute) else c.minute,
                        daysMask = if (intent.hasExtra("days")) intent.getIntExtra("days", c.daysMask) else c.daysMask,
                    )
                }
                Log.i("AutoSched", "debug config applied: ${app.repository.configFor(automation)}")
            } finally {
                pending.finish()
            }
        }
    }
}
