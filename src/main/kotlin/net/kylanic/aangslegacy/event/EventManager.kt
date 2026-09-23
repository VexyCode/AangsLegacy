package net.kylanic.aangslegacy.event

import net.kylanic.aangslegacy.AangsLegacy
import net.kylanic.aangslegacy.bender.BenderManager

object EventManager {
    private val eventList: MutableList<Event> = mutableListOf()

    fun sendEvent(event: Event) {
        AangsLegacy.logger.info(
            "Sent event: (${event.type.name}, by player ${event.player.name})"
        )

        eventList.add(event)
    }

    fun handleEvents() {
        for (event in eventList) {
            AangsLegacy.logger.info(
                "Handling (${event.type.name}, by player ${event.player.name})"
            )

            BenderManager.handleEvent(event)
        }

        clear()
    }

    private fun clear() {
        eventList.clear()
    }
}