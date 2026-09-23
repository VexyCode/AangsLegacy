package net.kylanic.aangslegacy.bender

import net.kylanic.aangslegacy.element.Element
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.util.UUID

class Bender(
    val id: UUID,
    val nativeElement: Element
) {
    lateinit var abilityManager: BenderAbilityManager

    fun onInit(player: Player) {
        abilityManager = BenderAbilityManager(player)

        abilityManager.setSelectedSlot(player.inventory.heldItemSlot)
    }

    fun save(players: YamlConfiguration): Unit {
        players.set("${id}.element", nativeElement.name)
    }

    fun tickCooldowns() {
        abilityManager.tickCooldowns()
    }

    fun resetCooldown(abilityId: String) {
        this.abilityManager.resetAbilityCooldown(abilityId)
    }
}