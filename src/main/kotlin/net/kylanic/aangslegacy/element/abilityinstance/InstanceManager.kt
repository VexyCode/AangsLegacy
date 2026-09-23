package net.kylanic.aangslegacy.element.abilityinstance

import net.kylanic.aangslegacy.AangsLegacy
import net.kylanic.aangslegacy.element.ability.AbilityRegistry
import net.kylanic.aangslegacy.event.Event
import org.bukkit.entity.Player

object InstanceManager {
    val instancedAbilities: MutableList<AbilityInstance> = mutableListOf()

    fun summonAbilityInstance(id: String, owner: Player): AbilityInstance? {

        val abilityInstance = AbilityRegistry.createInstance(id, owner) ?: return null

        instancedAbilities.add(abilityInstance)
        abilityInstance.start()
        return abilityInstance
    }

    fun tickAllAbilityInstances() {
        instancedAbilities.removeIf { ability ->
            ability.tick()
            !ability.isActive
        }
    }

    fun handleEvent(event: Event) {
        for (ability in instancedAbilities.toList()) {
            AangsLegacy.logger.info("Handling event (${event.type.name}, by player ${event.player.name}) by ability ${ability::class.simpleName}")
            ability.handleEvent(event)
        }
    }

    fun deleteAbilityInstance(instance: AbilityInstance): Boolean {
        if (instance !in instancedAbilities) return false
        instancedAbilities.remove(instance)
        return true
    }

    fun getAbilityInstance(
        id: String,
        owner: Player
    ): AbilityInstance? {
        return instancedAbilities.firstOrNull {
            it.id == id &&
            it.owner == owner &&
            it.isActive &&
            it !is KhaleAbilityInstance
        }
    }

    fun getAbilityInstances(id: String, owner: Player): List<AbilityInstance> {
        return instancedAbilities.filter {
            it.id == id && it.owner == owner && it.isActive
        }
    }
}