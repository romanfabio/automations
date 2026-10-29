package io.github.romanfabio.automations.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.voice.VoiceInteractionSession
import android.util.Log

/** Invisible assistant session: launches one voice-control settings activity, then hides itself. */
class ActionSession(context: Context) : VoiceInteractionSession(context) {

    override fun onCreate() {
        super.onCreate()
        // This session only launches an action; it must never draw an assistant panel.
        setUiEnabled(false)
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        when (val action = args?.getString(EXTRA_ACTION)) {
            ACTION_AIRPLANE_ON -> {
                val intent = Intent(Settings.ACTION_VOICE_CONTROL_AIRPLANE_MODE)
                    .addCategory(Intent.CATEGORY_VOICE)
                    .putExtra(Settings.EXTRA_AIRPLANE_MODE_ENABLED, true)
                Log.i(TAG, "session shown, starting voice activity")
                startVoiceActivity(intent)
            }
            else -> {
                Log.w(TAG, "unknown action $action")
                hide()
            }
        }
    }

    override fun onRequestCompleteVoice(request: CompleteVoiceRequest) {
        Log.i(TAG, "voice activity completed")
        request.sendCompleteResult(null)
        hide()
    }

    override fun onRequestAbortVoice(request: AbortVoiceRequest) {
        Log.w(TAG, "voice activity aborted")
        request.sendAbortResult(null)
        hide()
    }

    override fun onRequestConfirmation(request: ConfirmationRequest) {
        Log.i(TAG, "confirmation requested, auto-accepting")
        request.sendConfirmationResult(true, null)
    }

    companion object {
        private const val TAG = "AutoAssist"
        const val EXTRA_ACTION = "action"
        const val ACTION_AIRPLANE_ON = "airplane_on"
    }
}
