package net.kylanic.aangslegacy.listener

import io.papermc.paper.event.player.PlayerArmSwingEvent
import net.kylanic.aangslegacy.AangsLegacy
import net.kylanic.aangslegacy.bender.BenderManager
import net.kylanic.aangslegacy.event.Event
import net.kylanic.aangslegacy.event.EventManager
import net.kylanic.aangslegacy.event.EventType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class ArmSwingListener : Listener {

    @EventHandler
    fun onArmSwing(event: PlayerArmSwingEvent) {
        val player = event.player

        if (isBuilding(player)) {
            return
        }

        EventManager.sendEvent(
            Event(EventType.ArmSwing, player)
        )
    }

    private fun isBuilding(player: Player): Boolean {
        val item = player.inventory.itemInMainHand

        if (!item.type.isBlock) {
            return false
        }

        val target = player.getTargetBlockExact(5)

        return target != null
    }
}