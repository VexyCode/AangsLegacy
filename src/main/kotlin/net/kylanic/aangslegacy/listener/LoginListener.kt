package net.kylanic.aangslegacy.listener

import net.kylanic.aangslegacy.AangsLegacy
import net.kylanic.aangslegacy.player.BenderManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class LoginListener : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val id = player.uniqueId

        AangsLegacy.logger.info("${player.name} joined the server ($id)")
        BenderManager.createPlayer(player)
    }
}