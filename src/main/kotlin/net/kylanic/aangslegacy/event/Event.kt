package net.kylanic.aangslegacy.event

import org.bukkit.entity.Player

data class Event(
    val type: EventType,
    val player: Player,
)
