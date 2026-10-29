package io.github.romanfabio.automations.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import io.github.romanfabio.automations.MainActivity
import io.github.romanfabio.automations.automation.AutomationRegistry
import io.github.romanfabio.automations.automation.isScheduled
import io.github.romanfabio.automations.data.AutomationConfig
import java.time.ZonedDateTime

/**
 * One alarm-clock alarm per enabled scheduled automation. Alarm-clock alarms fire exactly, even in Doze
 * and battery saver, so no background service is needed. Only the next occurrence is ever armed: after
 * it fires (or after a reboot or clock change) the next one is computed, which also means missed runs
 * are simply skipped.
 */
class AlarmScheduler(private val context: Context, private val store: ScheduleStore) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Persists [config] to the mirror and arms or cancels the alarm accordingly. */
    fun apply(config: AutomationConfig) {
        store.put(config)
        arm(config)
    }

    /** Re-arms every automation from the mirror. Safe before the first unlock after a reboot. */
    fun rearmAllFromStore() {
        AutomationRegistry.all.filter { it.isScheduled }.forEach { automation ->
            store.get(automation.id)?.let(::arm)
        }
    }

    fun arm(config: AutomationConfig) {
        val next = if (config.enabled) {
            NextRunCalculator.next(config.hour, config.minute, config.daysMask, ZonedDateTime.now())
        } else {
            null
        }
        if (next == null) {
            cancel(config.id)
            return
        }
        val show = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(next.toInstant().toEpochMilli(), show), firePendingIntent(config.id))
        Log.i(TAG, "armed ${config.id} for $next")
    }

    fun cancel(id: String) {
        alarmManager.cancel(firePendingIntent(id))
        Log.i(TAG, "cancelled $id")
    }

    private fun firePendingIntent(id: String): PendingIntent {
        // The data URI makes the intent unique per automation, so each one has its own alarm.
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE)
            .setData(Uri.parse("automation://$id"))
            .putExtra(AlarmReceiver.EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val TAG = "AutoSched"
    }
}
