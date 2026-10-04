package net.kylanic.aangslegacy.element.abilityinstance

import net.kylanic.aangslegacy.element.ability.AbilityRegistry
import net.kylanic.aangslegacy.element.abilityinstance.instancetype.KhaleAbilityInstance
import org.bukkit.entity.Player

object InstanceManager {
    val instancedAbilities: MutableList<AbilityInstance> = mutableListOf()

    fun summonAbilityInstance(id: String, owner: Player): AbilityInstance? {
        val abilityInstance = AbilityRegistry.createInstance(id, owner) ?: return null

        instancedAbilities.add(abilityInstance)
        return abilityInstance
    }

    fun tickAllAbilityInstances() {
        instancedAbilities.removeIf { ability ->
            ability.tick()
            !ability.isActive
        }
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