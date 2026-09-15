package net.kylanic.aangslegacy.player

import net.kylanic.aangslegacy.AangsLegacy
import net.kylanic.aangslegacy.element.Element
import net.kyori.adventure.text.Component
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.UUID
import kotlin.random.Random

object BenderManager {
    private val benders: MutableMap<UUID, Bender> = mutableMapOf()

    fun createPlayer(player: Player) {
        val id = player.uniqueId
        if (this.has(id)) {
            AangsLegacy.logger.info("ID '$id' already tied to a bender.")
            return
        }

        val seed = id.mostSignificantBits xor
                id.leastSignificantBits xor
                System.currentTimeMillis()
        val random = Random(seed)

        val element: Element = when (val x = random.nextInt(0, 4)) {
            0 -> Element.Fire
            1 -> Element.Air
            2 -> Element.Water
            3 -> Element.Earth
            else -> {
                AangsLegacy.logger.info("Unable to select an element for '${id}' (random number: $x) defaulting to fire")
                Element.Fire
            }
        }

        val bender: Bender = Bender(id, element)
        this.benders[id] = bender

        AangsLegacy.logger.info("Created bender for ID '$id' with element $element.")
        player.sendMessage(Component.text("${element.toColorCode()}Hello, ${player.name}! Welcome to the server! You are a/an ${element}bender. Good luck!"))
    }

    fun has(id: UUID): Boolean = id in this.benders

    fun save() {
        AangsLegacy.logger.info("Saving players...")

        val file = File(AangsLegacy.dataFolder, "players.yml")

        if (!file.exists()) file.createNewFile()

        val players = YamlConfiguration.loadConfiguration(file)

        for ((id, b) in benders) {
            AangsLegacy.logger.info("\tSaving player '${id}'")
            b.save(players)
        }

        players.save(file)
    }

    fun load() {
        AangsLegacy.logger.info("Loading players!")
        val file = File(AangsLegacy.dataFolder, "players.yml")
        if (!file.exists()) {
            AangsLegacy.logger.warning("\tNo players.yml file found. Will create when saving.")
            return
        }

        val players = YamlConfiguration.loadConfiguration(file)

        for (id in players.getKeys(false)) {
            val uuid = UUID.fromString(id)
            val elementString = players.getString("$id.element")
            var element: Element? = null
            if (elementString == null) {
                AangsLegacy.logger.warning("\tPlayer id '$id' does not have an element attached. Defaulting to Fire.")
            } else {
                element = Element.fromString(elementString)
                if (element == null) {
                    AangsLegacy.logger.warning("\tUnknown element '$elementString'. Defaulting to Fire.")
                    element = Element.Fire
                }
            }

            benders[uuid] = Bender(uuid, element!!)
        }
    }

    fun get(id: UUID): Bender? = benders[id]

    fun get(player: Player): Bender? {
        val id = player.uniqueId
        return benders[id]
    }
}