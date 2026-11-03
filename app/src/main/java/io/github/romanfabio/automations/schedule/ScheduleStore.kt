package io.github.romanfabio.automations.schedule

import android.content.Context
import androidx.core.content.edit
import io.github.romanfabio.automations.data.AutomationConfig
import io.github.romanfabio.automations.data.RunLog
import io.github.romanfabio.automations.data.RunStatus
import io.github.romanfabio.automations.data.RunTrigger
import org.json.JSONArray
import org.json.JSONObject

/**
 * Mirror of the schedule in device-protected storage, which (unlike Room) is readable after a reboot
 * and before the first unlock. It is the only thing the alarm and boot receivers need to re-arm
 * alarms and run an automation. Also queues run-log entries produced while Room is unreadable.
 */
class ScheduleStore(context: Context) {
    private val prefs = context.createDeviceProtectedStorageContext()
        .getSharedPreferences("schedule", Context.MODE_PRIVATE)

    @Synchronized
    fun put(config: AutomationConfig) {
        // commit = true: receivers are short-lived, so the write must be on disk before they return.
        prefs.edit(commit = true) {
            putString(configKey(config.id), "${config.enabled}|${config.hour}|${config.minute}|${config.daysMask}")
        }
    }

    @Synchronized
    fun get(id: String): AutomationConfig? {
        val parts = prefs.getString(configKey(id), null)?.split('|') ?: return null
        if (parts.size != 4) return null
        return AutomationConfig(
            id = id,
            enabled = parts[0].toBoolean(),
            hour = parts[1].toIntOrNull() ?: return null,
            minute = parts[2].toIntOrNull() ?: return null,
            daysMask = parts[3].toIntOrNull() ?: return null,
        )
    }

    @Synchronized
    fun addPendingLog(log: RunLog) {
        val array = JSONArray(prefs.getString(PENDING_KEY, "[]"))
        array.put(
            JSONObject()
                .put("automationId", log.automationId)
                .put("timestamp", log.timestamp)
                .put("trigger", log.trigger.name)
                .put("status", log.status.name)
                .put("message", log.message),
        )
        prefs.edit(commit = true) { putString(PENDING_KEY, array.toString()) }
    }

    /** Returns and clears the queued entries. */
    @Synchronized
    fun takePendingLogs(): List<RunLog> {
        val array = JSONArray(prefs.getString(PENDING_KEY, "[]"))
        if (array.length() == 0) return emptyList()
        prefs.edit(commit = true) { remove(PENDING_KEY) }
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            RunLog(
                automationId = o.getString("automationId"),
                timestamp = o.getLong("timestamp"),
                trigger = RunTrigger.valueOf(o.getString("trigger")),
                status = RunStatus.valueOf(o.getString("status")),
                message = o.getString("message"),
            )
        }
    }

    private fun configKey(id: String) = "config_$id"

    private companion object {
        const val PENDING_KEY = "pending_logs"
    }
}
