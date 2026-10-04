package net.kylanic.aangslegacy.bender

import net.kylanic.aangslegacy.AangsLegacy
import net.kylanic.aangslegacy.element.ability.Ability
import net.kylanic.aangslegacy.element.ability.AbilityRegistry
import net.kylanic.aangslegacy.element.ability.ProgressingAbility
import net.kylanic.aangslegacy.element.progression.AbilityProgression
import net.kylanic.aangslegacy.element.abilityinstance.AbilityInstance
import net.kylanic.aangslegacy.element.abilityinstance.InstanceManager
import net.kylanic.aangslegacy.event.Event
import net.kylanic.aangslegacy.event.EventType
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import kotlin.math.roundToInt

class BenderAbilityManager(
    val player: Player,
    private val progression: AbilityProgression
) {
    val equippedAbilities: MutableList<Ability?> = MutableList(9) { null }
    private var selectedSlot: Int = 0

    private val cooldowns: MutableMap<String, Int> = mutableMapOf()

    fun getCooldown(abilityId: String): Int? = cooldowns[abilityId]

    private fun cooldownTicksFor(ability: Ability): Int {
        if (ability !is ProgressingAbility) return ability.cooldownTicks

        val level = progression.getLevel(ability.id)
        return (ability.cooldownTicks * level.cooldownMultiplier)
            .roundToInt()
            .coerceAtLeast(1)
    }

    fun setAbilitySlot(slot: Int, ability: Ability?): String? {
        if (slot !in 0..8) return "no"
        equippedAbilities[slot] = ability
        return null
    }

    fun getAbilitySlot(slot: Int): Ability? {
        if (slot !in 0..8) return null
        return equippedAbilities[slot]
    }

    fun setSelectedSlot(newSlot: Int): Boolean {
        if (newSlot !in 0..8) return false

        selectedSlot = newSlot

        val ability = getAbilitySlot(selectedSlot)
        ability?.let {
            val levelTag = if (it is ProgressingAbility)
                " §7[${progression.getLevel(it.id).displayName}]"
            else ""

            val remaining = getCooldown(it.id)

            if (remaining != null) {
                player.sendActionBar(Component.text("${it.name}${it.colorCode} - ${remaining / 20}s$levelTag"))
            } else {
                player.sendActionBar(Component.text("${it.name}$levelTag"))
            }
        }

        return true
    }

    fun getSelectedSlot(): Int = selectedSlot

    fun tickCooldowns() {
        val iterator = cooldowns.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()

            if (entry.value <= 1) iterator.remove()
            else entry.setValue(entry.value - 1)
        }
    }

    fun summonAbilityInstance(slot: Int): AbilityInstance? {
        val ability = getAbilitySlot(slot) ?: return null

        if (ability.id in cooldowns) return null

        val instance = InstanceManager.summonAbilityInstance(
            ability.id,
            player
        )

        cooldowns[ability.id] = cooldownTicksFor(ability)

        return instance
    }

    fun resetAbilityCooldown(abilityId: String) {
        val ability = equippedAbilities.firstOrNull { it?.id == abilityId } ?: return

        cooldowns[abilityId] = cooldownTicksFor(ability)
    }

    fun summonAbilityInstance(): AbilityInstance? =
        summonAbilityInstance(getSelectedSlot())

    fun handleEvent(event: Event) {
        when (event.type) {
            EventType.ArmSwing -> handleArmSwing(event)
            EventType.Shift -> handleShift(event)
        }
    }

    private fun handleArmSwing(event: Event) {
        if (event.player != player) return

        val abilityId = getAbilitySlot(getSelectedSlot())?.id ?: return

        if (AbilityRegistry.isShiftAbility(abilityId, player)) {
            val instances = InstanceManager.getAbilityInstances(abilityId, player)
            for (instance in instances) {
                instance.handleEvent(event)
            }
            return
        }

        val instance = InstanceManager.getAbilityInstance(
            abilityId,
            player
        )

        if (instance != null) {
            instance.handleEvent(event)
            return
        }

        summonAbilityInstance()?.handleEvent(event)
    }

    private fun handleShift(event: Event) {
        if (event.player != player) return

        val abilityId = getAbilitySlot(getSelectedSlot())?.id ?: return

        if (AbilityRegistry.isShiftAbility(abilityId, player)) {
            summonAbilityInstance()?.handleEvent(event)
            return
        }

        val instance = InstanceManager.getAbilityInstance(
            abilityId,
            player
        )

        if (instance != null) {
            instance.handleEvent(event)
            return
        }

        summonAbilityInstance()?.handleEvent(event)
    }

    fun getAbilityIds(): List<String> {
        val ids: MutableList<String> = mutableListOf()

        for (a in equippedAbilities) {
            ids.add(a?.id ?: "null")
        }

        return ids
    }
}