package net.kylanic.aangslegacy.listener

import net.kylanic.aangslegacy.AangsLegacy
import net.kylanic.aangslegacy.bender.BenderManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemHeldEvent

class HotbarSlotChangeListener : Listener {
    @EventHandler
    fun onHotbarChange(event: PlayerItemHeldEvent) {
        val player = event.player
        val newSlot = event.newSlot

        val bender = BenderManager.get(player)

        bender?.abilityManager?.setSelectedSlot(newSlot)
    }
}