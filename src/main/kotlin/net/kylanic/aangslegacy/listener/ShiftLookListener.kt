package net.kylanic.aangslegacy.listener

import net.kylanic.aangslegacy.event.Event
import net.kylanic.aangslegacy.event.EventManager
import net.kylanic.aangslegacy.event.EventType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSneakEvent

class ShiftLookListener : Listener {

    @EventHandler
    fun onToggleSneak(event: PlayerToggleSneakEvent) {
        val player = event.player

        if (!event.isSneaking) return

        EventManager.sendEvent(
            Event(EventType.Shift, player)
        )
    }
}