package net.kylanic.aangslegacy.element.ability

import net.kylanic.aangslegacy.element.Element

abstract class Ability {
    abstract val id: String
    abstract val name: String
    abstract val element: Element
    abstract val cooldownTicks: Int
    val colorCode: String
        get() = element.toColorCode()
}