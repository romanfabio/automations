package io.github.romanfabio.automations.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import io.github.romanfabio.automations.AutomationsApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms alarms after anything that clears or invalidates them: reboot (also before first unlock),
 * clock or time-zone change, app update.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as AutomationsApplication
        Log.i(TAG, "received ${intent.action}")

        // The device-protected mirror is always readable, so this works even while the phone is still locked.
        app.scheduler.rearmAllFromStore()

        if (!context.getSystemService(UserManager::class.java).isUserUnlocked) return

        // Unlocked: also reconcile with Room (the source of truth) and import runs logged while locked.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.manager.resyncAll()
            } catch (e: Exception) {
                Log.e(TAG, "resync failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AutoSched"
    }
}
