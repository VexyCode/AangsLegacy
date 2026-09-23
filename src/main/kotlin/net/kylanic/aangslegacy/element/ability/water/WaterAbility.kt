package net.kylanic.aangslegacy.element.ability.water

import net.kylanic.aangslegacy.element.Element
import net.kylanic.aangslegacy.element.ability.Ability

abstract class WaterAbility : Ability() {
    override val element: Element
        get() = Element.Water
}