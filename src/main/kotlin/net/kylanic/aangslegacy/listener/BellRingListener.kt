package net.kylanic.aangslegacy.listener

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kylanic.aangslegacy.bender.BenderManager
import net.kylanic.aangslegacy.element.Element
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BellRingEvent

class BellRingListener : Listener {
    @EventHandler
    fun onBellRing(event: BellRingEvent) {
        val player = event.entity as? Player ?: return
        val bender = BenderManager.get(player) ?: return
        val loc = event.block.location

        val element = Element.entries.firstOrNull { inTemple(it, player) } ?: return

        if (bender.usedTemple(loc)) {
            player.sendActionBar(
                Component.text(
                    "You already used this temple, find a new one to increase your element attunement even further."
                )
            )
            return
        }

        bender.useTemple(loc)
        bender.increaseAttunement(element, player)
    }

    private fun inTemple(element: Element, player: Player): Boolean {
        val structure = RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.STRUCTURE)
            .get(NamespacedKey("aangslegacy", "${element.name.lowercase()}_temple"))
            ?: return false

        val loc = player.location
        return player.world
            .getStructures(loc.blockX shr 4, loc.blockZ shr 4, structure)
            .any { it.boundingBox.contains(loc.toVector()) }
    }
}