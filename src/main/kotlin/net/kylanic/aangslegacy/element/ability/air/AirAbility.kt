package net.kylanic.aangslegacy.element.ability.air

import net.kylanic.aangslegacy.element.Element
import net.kylanic.aangslegacy.element.ability.Ability

abstract class AirAbility : Ability() {
    override val element: Element
        get() = Element.Air
}