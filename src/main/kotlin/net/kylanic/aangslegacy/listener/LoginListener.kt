package net.kylanic.aangslegacy.listener

import net.kylanic.aangslegacy.AangsLegacy
import net.kylanic.aangslegacy.bender.BenderManager
import net.kylanic.aangslegacy.element.Element
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.io.File

class LoginListener : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val id = player.uniqueId

        val file = File(AangsLegacy.dataFolder, "players/${id}.yml")

        if (!file.exists()) {
            file.createNewFile()
        }

        val playerConfig = YamlConfiguration.loadConfiguration(file)

        AangsLegacy.logger.info("${player.name} joined the server ($id)")
        BenderManager.createPlayer(player)

        val elementAttunement = mutableMapOf(
            Element.Fire to 0,
            Element.Air to 0,
            Element.Water to 0,
            Element.Earth to 0
        )

        val elements = playerConfig.getConfigurationSection("element_attunements")
        if (elements != null) {
            for (el in elements.getKeys(false)) {
                val attunement = elements.getInt(el)
                val element = Element.fromString(el)

                if (element != null) {
                    elementAttunement[element] = attunement
                }
            }
        }

        val bender = BenderManager.get(id)
        bender?.onInit(player, elementAttunement)
    }
}