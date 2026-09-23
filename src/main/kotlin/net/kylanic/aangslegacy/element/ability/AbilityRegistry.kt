package net.kylanic.aangslegacy.element.ability

import net.kylanic.aangslegacy.AangsLegacy
import net.kylanic.aangslegacy.element.ability.air.AirScythe
import net.kylanic.aangslegacy.element.ability.earth.EarthKick
import net.kylanic.aangslegacy.element.ability.fire.Fireball
import net.kylanic.aangslegacy.element.ability.instance.AbilityInstance
import net.kylanic.aangslegacy.element.ability.instance.ShiftAbility
import net.kylanic.aangslegacy.element.ability.instance.air.AirScytheInstance
import net.kylanic.aangslegacy.element.ability.instance.earth.EarthKickInstance
import net.kylanic.aangslegacy.element.ability.instance.fire.FireballInstance
import net.kylanic.aangslegacy.element.ability.instance.water.WaterSplashInstance
import net.kylanic.aangslegacy.element.ability.water.WaterSplash
import net.kylanic.aangslegacy.util.AbilityRegistryEntry
import org.bukkit.entity.Player

object AbilityRegistry {
    private val registry: MutableMap<String, AbilityRegistryEntry> = mutableMapOf(
        "al:fireball" to AbilityRegistryEntry(Fireball()) { player -> FireballInstance(player) },
        "al:water_splash" to AbilityRegistryEntry(WaterSplash()) { player -> WaterSplashInstance(player) },
        "al:earth_kick" to AbilityRegistryEntry(EarthKick()) { player -> EarthKickInstance(player) },
        "al:air_scythe" to AbilityRegistryEntry(AirScythe()) { player -> AirScytheInstance(player) }
    )

    fun getRegistryEntry(id: String): AbilityRegistryEntry? {
        if (id !in registry) return null
        return registry[id]
    }

    fun getAbilityDefinition(id: String): Ability? {
        if (id !in registry) return null
        return registry[id]?.ability
    }

    fun createInstance(id: String, player: Player): AbilityInstance? {
        val entry = registry[id] ?: return null
        return entry.instanceFactory(player)
    }

    fun getAllIds(): List<String> {
        val ids: MutableList<String> = mutableListOf()
        for ((id, _) in registry) ids.add(id)

        return ids
    }

    fun isShiftAbility(id: String, player: Player): Boolean {
        val throwaway = createInstance(id, player) ?: return false
        return throwaway is ShiftAbility
    }
}