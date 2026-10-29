package io.github.romanfabio.automations.automation

import io.github.romanfabio.automations.automation.airplane.AirplaneOnAutomation

object AutomationRegistry {
    val all: List<Automation> = listOf(
        AirplaneOnAutomation(),
    )

    fun byId(id: String): Automation? = all.firstOrNull { it.id == id }
}
