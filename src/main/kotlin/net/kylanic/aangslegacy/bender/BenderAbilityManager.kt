package net.kylanic.aangslegacy.bender

import net.kylanic.aangslegacy.AangsLegacy
import net.kylanic.aangslegacy.element.ability.Ability
import net.kylanic.aangslegacy.element.ability.AbilityRegistry
import net.kylanic.aangslegacy.element.abilityinstance.AbilityInstance
import net.kylanic.aangslegacy.element.abilityinstance.InstanceManager
import net.kylanic.aangslegacy.event.Event
import net.kylanic.aangslegacy.event.EventType
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

class BenderAbilityManager(
    val player: Player
) {
    val equippedAbilities: MutableList<Ability?> = MutableList(9) { null }
    private var selectedSlot: Int = 0

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
            if (it.cooldown != null) {
                player.sendActionBar(Component.text("${it.name}${it.colorCode} - ${it.cooldown!! / 20}s"))
            } else {
                player.sendActionBar(Component.text(it.name))
            }
        }

        return true
    }

    fun getSelectedSlot(): Int = selectedSlot

    fun tickCooldowns() {
        for (ability in equippedAbilities) {
            ability?.cooldown?.let {
                ability.cooldown = if (it <= 1) null else it - 1
            }
        }
    }

    fun summonAbilityInstance(slot: Int): AbilityInstance? {
        val ability = getAbilitySlot(slot) ?: return null

        if (ability.cooldown != null) return null

        val instance = InstanceManager.summonAbilityInstance(
            ability.id,
            player
        )

        ability.cooldown = ability.cooldownTicks

        return instance
    }

    fun resetAbilityCooldown(abilityId: String) {
        for (ability in equippedAbilities) {
            if (ability?.id != abilityId) continue

            ability.cooldown = ability.cooldownTicks
        }
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