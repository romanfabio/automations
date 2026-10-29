package io.github.romanfabio.automations.setup

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import io.github.romanfabio.automations.automation.Automation

/** One line of the setup checklist. */
data class SetupItem(
    val id: String,
    val title: String,
    val detail: String,
    val met: Boolean,
    /** System screen that lets the user fix it. */
    val intent: Intent? = null,
    /** Fixed by the runtime notification permission dialog instead of [intent]. */
    val needsNotificationPermission: Boolean = false,
    /** Can't be detected from code; the user confirms it with "Done". */
    val manual: Boolean = false,
)

/**
 * Everything that must be right on the phone for scheduled automations to keep working:
 * what each automation requires, notifications, battery optimization, and the OEM background rules.
 */
class SetupChecker(private val context: Context) {
    private val prefs = context.getSharedPreferences("setup", Context.MODE_PRIVATE)
    private val packageName = context.packageName

    fun items(automations: List<Automation>): List<SetupItem> = buildList {
        automations.flatMap { it.requirements() }.distinctBy { it.title }.forEach { requirement ->
            add(
                SetupItem(
                    id = "requirement:${requirement.title}",
                    title = requirement.title,
                    detail = requirement.howToFix,
                    met = requirement.isMet(context),
                    intent = requirement.fixIntent,
                ),
            )
        }
        add(
            SetupItem(
                id = "notifications",
                title = "Notifications",
                detail = "Needed to tell you when an automation fails.",
                met = context.getSystemService(NotificationManager::class.java).areNotificationsEnabled(),
                needsNotificationPermission = true,
            ),
        )
        add(
            SetupItem(
                id = "battery",
                title = "Unrestricted battery use",
                detail = "Stops battery optimization from delaying or blocking scheduled runs.",
                met = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName),
                intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$packageName")),
            ),
        )
        if (isOplusFamily()) {
            val appInfo = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:$packageName"))
            add(manual("oem_autolaunch", "Allow auto launch", "App info > Battery usage > turn on \"Allow auto launch\".", appInfo))
            add(manual("oem_background", "Allow background activity", "App info > Battery usage > turn on \"Allow background activity\".", appInfo))
            add(manual("oem_recents", "Lock in Recents", "Open Recents, long-press the Automations card, choose Lock, so cleaning apps doesn't kill it."))
        }
    }

    fun markManualDone(id: String) {
        prefs.edit().putBoolean(manualKey(id), true).apply()
    }

    private fun manual(id: String, title: String, detail: String, intent: Intent? = null) = SetupItem(
        id = id,
        title = title,
        detail = detail,
        met = prefs.getBoolean(manualKey(id), false),
        intent = intent,
        manual = true,
    )

    private fun manualKey(id: String) = "manual_$id"

    private fun isOplusFamily(): Boolean =
        Build.MANUFACTURER.lowercase() in setOf("realme", "oppo", "oneplus")
}
