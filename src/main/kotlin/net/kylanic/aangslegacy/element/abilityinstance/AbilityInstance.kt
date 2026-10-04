package net.kylanic.aangslegacy.element.abilityinstance

import net.kylanic.aangslegacy.bender.Bender
import net.kylanic.aangslegacy.bender.BenderManager
import net.kylanic.aangslegacy.element.ability.AbilityRegistry
import net.kylanic.aangslegacy.element.ability.ProgressingAbility
import net.kylanic.aangslegacy.element.progression.AbilityLevel
import net.kylanic.aangslegacy.event.Event
import net.kyori.adventure.text.Component
import org.bukkit.Sound
import org.bukkit.entity.Player

abstract class AbilityInstance(
    open val id: String,
    open val owner: Player
) {
    var isActive: Boolean = false

    protected val bender: Bender?
        get() = BenderManager.get(owner)

    protected val level: AbilityLevel
        get() = bender?.progression?.getLevel(id) ?: AbilityLevel.LEARNED

    protected fun awardXp(amount: Int? = null) {
        val definition = AbilityRegistry.getAbilityDefinition(id)
        val gain = amount ?: (definition as? ProgressingAbility)?.xpPerHit ?: return

        val newLevel = bender?.progression?.addXp(id, gain) ?: return

        owner.sendActionBar(
            Component.text("§aLevel up! ${definition?.name ?: id} §ais now §e${newLevel.displayName}§a.")
        )
        owner.playSound(owner.location, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f)
    }

    fun start() {
        if (isActive) return
        isActive = true

        onStart()
    }

    fun tick() {
        if (!isActive) return
        onTick()
    }

    fun end() {
        if (isActive) isActive = false
        onEnd()
    }

    fun handleEvent(event: Event) {
        onEvent(event)
    }

    protected abstract fun onStart()
    protected abstract fun onTick()
    protected abstract fun onEnd()
    protected abstract fun onEvent(event: Event)
}
