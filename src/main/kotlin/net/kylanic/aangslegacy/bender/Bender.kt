package net.kylanic.aangslegacy.bender

import net.kylanic.aangslegacy.AangsLegacy
import net.kylanic.aangslegacy.element.Element
import net.kylanic.aangslegacy.element.ability.AbilityRegistry
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.util.Locale.getDefault
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

class Bender(
    val id: UUID,
    val nativeElement: Element,
    val unlockedElements: MutableList<Element>,
    val abilityIds: MutableList<String?>,
) {
    var lastElement: Element = nativeElement

    val nextElement: Element
        get() = lastElement.getNextInElementCycle()

    val elementAttunement = mutableMapOf(
        Element.Fire to 0,
        Element.Air to 0,
        Element.Water to 0,
        Element.Earth to 0
    )

    val usedTemples = mutableListOf<Location>()

    lateinit var abilityManager: BenderAbilityManager

    fun info(playerName: String): String {
        val bendings = mutableListOf<String>()

        for ((key, value) in elementAttunement)
            if (value >= 3)
                bendings.add("- ${key.name.lowercase()}bender")

        return """
            Player $playerName is a(n):
            ${bendings.joinToString { " $it\n" }}
            
        """.trimIndent()
    }

    fun getAttunementInfo(): String {
        val atts = mutableListOf<String>()

        for ((key, value) in elementAttunement)
            atts.add(" - ${key.name}: $value")

        return atts.joinToString { " $it\n" }
    }

    fun getAttunementInfo(element: Element): String {
        val att = elementAttunement[element]

        return "${element.name} attunement: $att"
    }

    fun onInit(player: Player, attunement: Map<Element, Int>? = null) {
        abilityManager = BenderAbilityManager(player)

        for ((slot, abilityId) in abilityIds.withIndex()) {
            if (abilityId == null) continue

            val ability = AbilityRegistry.getAbilityDefinition(abilityId)

            if (ability == null) {
                AangsLegacy.logger.warning(
                    "Unknown ability '$abilityId' for player '$id' in slot $slot."
                )
                continue
            }

            abilityManager.setAbilitySlot(slot, ability)
        }

        abilityManager.setSelectedSlot(player.inventory.heldItemSlot)

        if (attunement != null)
            elementAttunement.putAll(attunement)

        if (unlockedElements.isEmpty()) {
            unlockedElements.add(nativeElement)
            elementAttunement[nativeElement] = 3
        } else {
            for (element in unlockedElements)
                elementAttunement[element] = 3

            elementAttunement[nativeElement] = 3
        }
    }

    fun save(players: YamlConfiguration) {
        if (!::abilityManager.isInitialized) return

        players.set("native_element", nativeElement.name)

        val ids = abilityManager.getAbilityIds()

        for ((slot, abilityId) in ids.withIndex())
            players.set("abilities.$slot", abilityId)

        for ((element, attunement) in elementAttunement) {
            players.set(
                "element_attunements.${element.name}",
                attunement
            )
        }

        players.set(
            "used_temple_locations",
            usedTemples
        )
    }

    fun loadAbilities(players: YamlConfiguration, player: Player) {
        abilityManager = BenderAbilityManager(player)

        for (slot in 0..8) {
            val abilityId = players.getString("${id}.abilities.$slot")

            if (abilityId == null || abilityId == "null") {
                abilityManager.setAbilitySlot(slot, null)
                continue
            }

            val ability = AbilityRegistry.getAbilityDefinition(abilityId)

            if (ability == null) {
                AangsLegacy.logger.warning(
                    "Unknown ability '$abilityId' for player '$id' in slot $slot."
                )
                continue
            }

            abilityManager.setAbilitySlot(slot, ability)
        }

        abilityManager.setSelectedSlot(player.inventory.heldItemSlot)

        if (unlockedElements.isEmpty())
            unlockedElements.add(nativeElement)
    }

    fun tickCooldowns() {
        abilityManager.tickCooldowns()
    }

    fun resetCooldown(abilityId: String) {
        abilityManager.resetAbilityCooldown(abilityId)
    }

    fun increaseAttunement(element: Element, player: Player) {
        if (element in unlockedElements) return

        val current = elementAttunement[element] ?: 0

        if (current >= 3) return

        val attunement = current + 1
        elementAttunement[element] = attunement

        // Small feedback for every attunement increase.
        playAttunementEffect(player, element, attunement)

        if (attunement < 3) {
            player.sendActionBar(
                Component.text(
                    "${element.toColorCode()}${element.name} attunement increased to ${attunement}/3."
                )
            )
            return
        }

        if (element == nextElement) {
            unlockedElements.add(element)
            lastElement = element

            playUnlockEffect(player, element)

            player.sendActionBar(
                Component.text(
                    "${element.toColorCode()}You can now bend ${element.name.lowercase(getDefault())}"
                )
            )

            for ((el, at) in elementAttunement) {
                if (at >= 3 && el !in unlockedElements) {
                    unlockedElements.add(el)

                    playUnlockEffect(player, el)

                    player.sendMessage(
                        Component.text(
                            "${el.toColorCode()}You can now also bend ${el.name.lowercase(getDefault())}"
                        )
                    )
                }
            }
        } else {
            player.sendActionBar(
                Component.text(
                    "${element.toColorCode()}You have learned ${element.name.lowercase(getDefault())}bending, " +
                            "but you cannot use it yet. Learn ${nextElement.name.lowercase(getDefault())} first."
                )
            )
        }
    }

    fun unlockElement(element: Element, player: Player) {
        if (element in unlockedElements) return

        elementAttunement[element] = 3
        unlockedElements.add(element)

        lastElement = element

        playUnlockEffect(player, element)

        player.sendActionBar(
            Component.text(
                "${element.toColorCode()}You can now bend ${element.name.lowercase(getDefault())}"
            )
        )
    }

    fun lockElement(element: Element, player: Player) {
        if (element !in unlockedElements) return

        elementAttunement[element] = 0
        unlockedElements.remove(element)

        lastElement = element.getPreviousElementCycle()

        playLockEffect(player, element)

        player.sendActionBar(
            Component.text(
                "${element.toColorCode()}The gods of bending decided you cannot bend ${element.name.lowercase(getDefault())} anymore"
            )
        )
    }

    private fun playAttunementEffect(
        player: Player,
        element: Element,
        level: Int
    ) {
        val loc = player.location.clone().add(0.0, 1.0, 0.0)

        spawnElementParticles(
            player,
            element,
            loc,
            count = 8 + level * 3,
            radius = 0.45
        )

        player.playSound(
            player.location,
            Sound.BLOCK_ENCHANTMENT_TABLE_USE,
            0.7f,
            1.0f + level * 0.1f
        )
    }

    private fun playUnlockEffect(
        player: Player,
        element: Element
    ) {
        val center = player.location.clone().add(0.0, 1.0, 0.0)

        // Expanding rings.
        for (ring in 0..2) {
            val radius = 0.6 + ring * 0.45

            for (i in 0 until 24) {
                val angle = (Math.PI * 2 * i) / 24

                val loc = center.clone().add(
                    cos(angle) * radius,
                    ring * 0.25,
                    sin(angle) * radius
                )

                spawnElementParticles(
                    player,
                    element,
                    loc,
                    count = 1,
                    radius = 0.0
                )
            }
        }

        // Vertical burst.
        spawnElementParticles(
            player,
            element,
            center,
            count = 45,
            radius = 0.8
        )

        player.playSound(
            player.location,
            Sound.BLOCK_BEACON_ACTIVATE,
            1.0f,
            1.15f
        )

        player.playSound(
            player.location,
            Sound.ENTITY_PLAYER_LEVELUP,
            0.8f,
            1.3f
        )
    }

    private fun playLockEffect(
        player: Player,
        element: Element
    ) {
        val center = player.location.clone().add(0.0, 1.0, 0.0)

        spawnElementParticles(
            player,
            element,
            center,
            count = 30,
            radius = 0.6
        )

        player.playSound(
            player.location,
            Sound.BLOCK_BEACON_DEACTIVATE,
            0.8f,
            0.8f
        )
    }

    private fun spawnElementParticles(
        player: Player,
        element: Element,
        location: Location,
        count: Int,
        radius: Double
    ) {
        when (element) {
            Element.Fire -> {
                player.spawnParticle(
                    Particle.FLAME,
                    location,
                    count,
                    radius,
                    radius,
                    radius,
                    0.03
                )

                player.spawnParticle(
                    Particle.LAVA,
                    location,
                    count / 4,
                    radius,
                    radius,
                    radius,
                    0.0
                )
            }

            Element.Air -> {
                player.spawnParticle(
                    Particle.CLOUD,
                    location,
                    count,
                    radius,
                    radius,
                    radius,
                    0.08
                )
            }

            Element.Water -> {
                player.spawnParticle(
                    Particle.SPLASH,
                    location,
                    count,
                    radius,
                    radius,
                    radius,
                    0.2
                )

                player.spawnParticle(
                    Particle.DRIPPING_WATER,
                    location,
                    count / 2,
                    radius,
                    radius,
                    radius,
                    0.0
                )
            }

            Element.Earth -> {
                player.spawnParticle(
                    Particle.BLOCK,
                    location,
                    count,
                    radius,
                    radius,
                    radius,
                    0.05,
                    Material.DIRT.createBlockData()
                )

                player.spawnParticle(
                    Particle.FALLING_DUST,
                    location,
                    count / 2,
                    radius,
                    radius,
                    radius,
                    0.02,
                    Material.DIRT.createBlockData()
                )
            }
        }
    }

    fun useTemple(loc: Location) {
        usedTemples.add(loc)
    }

    fun usedTemple(loc: Location): Boolean =
        loc in usedTemples
}
