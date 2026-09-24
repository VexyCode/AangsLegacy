package net.kylanic.aangslegacy.bender

import net.kylanic.aangslegacy.AangsLegacy
import net.kylanic.aangslegacy.element.Element
import net.kylanic.aangslegacy.event.Event
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
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
                AangsLegacy.logger.info(
                    "Unable to select an element for '$id' " +
                            "(random number: $x) defaulting to fire"
                )
                Element.Fire
            }
        }

        val bender = Bender(
            id,
            element,
            mutableListOf(),
            MutableList(9) { null }
        )

        benders[id] = bender

        AangsLegacy.logger.info(
            "Created bender for ID '$id' with element $element."
        )

        player.sendMessage(
            Component.text(
                "${element.toColorCode()}Hello, ${player.name}! " +
                        "Welcome to the server! You are a/an ${element}bender. Good luck!"
            )
        )
    }

    fun has(id: UUID): Boolean = id in benders

    fun save() {
        AangsLegacy.logger.info("Saving players...")

        val file = File(AangsLegacy.dataFolder, "players.yml")

        if (!file.exists()) {
            file.createNewFile()
        }

        val players = YamlConfiguration.loadConfiguration(file)

        for ((id, bender) in benders) {
            AangsLegacy.logger.info("\tSaving player '$id'")
            bender.save(players)
        }

        players.save(file)
    }

    fun load() {
        AangsLegacy.logger.info("Loading players!")

        val file = File(AangsLegacy.dataFolder, "players.yml")

        if (!file.exists()) {
            AangsLegacy.logger.warning(
                "\tNo players.yml file found. Will create when saving."
            )
            return
        }

        val players = YamlConfiguration.loadConfiguration(file)

        for (id in players.getKeys(false)) {
            val uuid = try {
                UUID.fromString(id)
            } catch (_: IllegalArgumentException) {
                AangsLegacy.logger.warning(
                    "\tInvalid player UUID '$id'. Skipping."
                )
                continue
            }

            val elementString = players.getString("$id.element")

            val element = if (elementString == null) {
                AangsLegacy.logger.warning(
                    "\tPlayer id '$id' does not have an element attached. " +
                            "Defaulting to Fire."
                )
                Element.Fire
            } else {
                Element.fromString(elementString) ?: run {
                    AangsLegacy.logger.warning(
                        "\tUnknown element '$elementString'. Defaulting to Fire."
                    )
                    Element.Fire
                }
            }

            val abilityIds = MutableList<String?>(9) { slot ->
                players.getString("$id.abilities.$slot")
            }

            val bender = Bender(
                uuid,
                element,
                mutableListOf(),
                abilityIds
            )

            benders[uuid] = bender

            AangsLegacy.logger.info(
                "\tLoaded player '$uuid' with element $element."
            )
        }
    }

    fun get(id: UUID): Bender? = benders[id]

    fun get(player: Player): Bender? {
        return benders[player.uniqueId]
    }

    fun handleEvent(event: Event) {
        val bender = benders[event.player.uniqueId] ?: return

        bender.abilityManager.handleEvent(event)
    }

    fun tickCooldowns() {
        for ((id, bender) in benders) {
            Bukkit.getPlayer(id) ?: continue
            bender.tickCooldowns()
        }
    }

    fun resetCooldown(player: Player, abilityId: String) {
        val bender = get(player) ?: return
        bender.resetCooldown(abilityId)
    }
}