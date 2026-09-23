package net.kylanic.aangslegacy.util

import net.kylanic.aangslegacy.element.ability.Ability
import net.kylanic.aangslegacy.element.abilityinstance.AbilityInstance
import org.bukkit.entity.Player

data class AbilityRegistryEntry(
    val ability: Ability,
    val instanceFactory: (Player) -> AbilityInstance
)
