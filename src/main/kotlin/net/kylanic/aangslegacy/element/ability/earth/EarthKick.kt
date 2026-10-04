package net.kylanic.aangslegacy.element.ability.earth

import net.kylanic.aangslegacy.element.ability.ProgressingAbility

class EarthKick(override val xpPerHit: Int = 5) : EarthAbility(), ProgressingAbility {
    override val id: String
        get() = "al:earth_kick"

    override val name: String
        get() = "${element.toColorCode()}Earth Kick§r"

    override val cooldownTicks: Int
        get() = 20 // 1 sec cooldown
}