package net.kylanic.aangslegacy.element.ability.earth

import net.kylanic.aangslegacy.element.Element
import net.kylanic.aangslegacy.element.ability.Ability

abstract class EarthAbility : Ability() {
    override val element: Element
        get() = Element.Earth
}