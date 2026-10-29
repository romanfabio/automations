package io.github.romanfabio.automations.automation.airplane

import android.app.role.RoleManager
import android.content.Context
import android.provider.Settings
import io.github.romanfabio.automations.assistant.ActionSession
import io.github.romanfabio.automations.assistant.AutomationsVoiceInteractionService
import io.github.romanfabio.automations.automation.Automation
import io.github.romanfabio.automations.automation.Requirement
import io.github.romanfabio.automations.automation.RunResult
import io.github.romanfabio.automations.automation.Schedule
import kotlinx.coroutines.delay

/**
 * Turns airplane mode on. Android 15 doesn't let apps write the setting, so the request goes through
 * the digital-assistant session, which may launch the system's voice-control airplane activity.
 */
class AirplaneOnAutomation : Automation {
    override val id = "airplane_on"
    override val title = "Airplane mode on"
    override val description = "Turns airplane mode on. Does nothing if it is already on."
    override val defaultSchedule = Schedule(hour = 23, minute = 0)

    override fun requirements() = listOf(
        Requirement(
            title = "Default digital assistant",
            howToFix = "Settings > Apps > Default apps > Digital assistant app: choose Automations.",
            isMet = { context ->
                context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_ASSISTANT)
            },
        ),
    )

    override suspend fun run(context: Context): RunResult {
        if (isAirplaneOn(context)) return RunResult.Skipped("Already in airplane mode")
        // After the process was killed, the system re-binds the assistant service asynchronously.
        var waited = 0
        while (AutomationsVoiceInteractionService.instance == null && waited < SERVICE_WAIT_ATTEMPTS) {
            delay(POLL_INTERVAL_MS)
            waited++
        }
        if (!AutomationsVoiceInteractionService.requestAction(ActionSession.ACTION_AIRPLANE_ON)) {
            return RunResult.Failed("Assistant service not active: set Automations as the default digital assistant")
        }
        repeat(POLL_ATTEMPTS) {
            delay(POLL_INTERVAL_MS)
            if (isAirplaneOn(context)) return RunResult.Success("Airplane mode turned on")
        }
        return RunResult.Failed("Airplane mode was not applied")
    }

    private fun isAirplaneOn(context: Context): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1

    private companion object {
        const val SERVICE_WAIT_ATTEMPTS = 25
        const val POLL_ATTEMPTS = 25
        const val POLL_INTERVAL_MS = 200L
    }
}
