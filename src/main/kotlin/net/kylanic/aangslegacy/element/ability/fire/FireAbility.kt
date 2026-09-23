package net.kylanic.aangslegacy.element.ability.fire

import net.kylanic.aangslegacy.element.Element
import net.kylanic.aangslegacy.element.ability.Ability

abstract class FireAbility : Ability() {
    override val element: Element
        get() = Element.Fire
}