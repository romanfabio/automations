package io.github.romanfabio.automations.assistant

import android.content.Intent
import android.speech.RecognitionService

/** Never used for speech; Android only lists a voice-interaction service as selectable if it declares one. */
class StubRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) = Unit
    override fun onCancel(listener: Callback?) = Unit
    override fun onStopListening(listener: Callback?) = Unit
}
