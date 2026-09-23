package net.kylanic.aangslegacy.element.ability.earth

class EarthKick : EarthAbility() {
    override val id: String
        get() = "al:earth_kick"

    override val name: String
        get() = "${element.toColorCode()}Earth Kick§r"

    override val cooldownTicks: Int
        get() = 20 // 1 sec cooldown
}