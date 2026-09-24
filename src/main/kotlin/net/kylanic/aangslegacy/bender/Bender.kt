package net.kylanic.aangslegacy.bender

import net.kylanic.aangslegacy.AangsLegacy
import net.kylanic.aangslegacy.element.Element
import net.kylanic.aangslegacy.element.ability.AbilityRegistry
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.util.UUID

class Bender(
    val id: UUID,
    val nativeElement: Element,
    val unlockedElements: MutableList<Element>,
    val abilityIds: MutableList<String?>
) {
    lateinit var abilityManager: BenderAbilityManager

    fun onInit(player: Player) {
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

        if (unlockedElements.isEmpty()) {
            unlockedElements.add(nativeElement)
        }
    }

    fun save(players: YamlConfiguration): Unit {
        if (!::abilityManager.isInitialized) return

        players.set("${id}.element", nativeElement.name)

        val ids = abilityManager.getAbilityIds()
        for ((index, id) in ids.withIndex()) {
            players.set("${this.id}.abilities.$index", id)
        }
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

        if (unlockedElements.isEmpty()) {
            unlockedElements.add(nativeElement)
        }
    }

    fun tickCooldowns() {
        abilityManager.tickCooldowns()
    }

    fun resetCooldown(abilityId: String) {
        this.abilityManager.resetAbilityCooldown(abilityId)
    }
}