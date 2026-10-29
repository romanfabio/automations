package io.github.romanfabio.automations.assistant

import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.util.Log

/**
 * Bound by the system for as long as this app is the default digital assistant.
 * [instance] is non-null only while bound, which doubles as a "do we hold the role" check.
 */
class AutomationsVoiceInteractionService : VoiceInteractionService() {

    override fun onReady() {
        super.onReady()
        instance = this
        Log.i(TAG, "assistant service ready")
    }

    override fun onShutdown() {
        if (instance === this) instance = null
        Log.i(TAG, "assistant service shutdown")
        super.onShutdown()
    }

    companion object {
        private const val TAG = "AutoSpike"

        @Volatile
        var instance: AutomationsVoiceInteractionService? = null
            private set

        /** Starts an assistant session that performs [action]; false if the service isn't bound. */
        fun requestAction(action: String): Boolean {
            val service = instance ?: return false
            service.showSession(Bundle().apply { putString(ActionSession.EXTRA_ACTION, action) }, 0)
            return true
        }
    }
}
