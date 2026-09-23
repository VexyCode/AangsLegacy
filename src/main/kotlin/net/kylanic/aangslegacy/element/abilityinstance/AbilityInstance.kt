package net.kylanic.aangslegacy.element.ability.instance

import net.kylanic.aangslegacy.bender.BenderManager
import net.kylanic.aangslegacy.event.Event
import org.bukkit.entity.Player

abstract class AbilityInstance(
    open val id: String,
    open val owner: Player
) {
    var isActive: Boolean = false

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