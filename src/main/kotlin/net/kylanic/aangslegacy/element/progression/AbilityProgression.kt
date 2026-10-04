package net.kylanic.aangslegacy.element.progression

import org.bukkit.configuration.file.YamlConfiguration

/**
 * One player's XP for every ability they've used.
 * Lives on the Bender. Abilities themselves stay stateless.
 */
class AbilityProgression {
    private val xp: MutableMap<String, Int> = mutableMapOf()

    fun getXp(abilityId: String): Int = xp[abilityId] ?: 0

    fun setXp(abilityId: String, value: Int) {
        xp[abilityId] = value.coerceAtLeast(0)
    }

    fun getLevel(abilityId: String): AbilityLevel =
        AbilityLevel.fromXP(getXp(abilityId))

    fun xpToNextLevel(abilityId: String): Int? {
        val next = getLevel(abilityId).next ?: return null
        return next.requiredXp - getXp(abilityId)
    }

    fun addXp(abilityId: String, amount: Int): AbilityLevel? {
        if (amount <= 0) return null

        val before = getLevel(abilityId)
        xp[abilityId] = getXp(abilityId) + amount
        val after = getLevel(abilityId)

        return if (after != before) after else null
    }

    fun save(config: YamlConfiguration) {
        for ((id, value) in xp)
            config.set("ability_xp.$id", value)
    }

    fun load(config: YamlConfiguration) {
        val section = config.getConfigurationSection("ability_xp") ?: return

        for (id in section.getKeys(false))
            setXp(id, section.getInt(id))
    }
}
