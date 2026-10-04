package net.kylanic.aangslegacy.element.ability.air

import net.kylanic.aangslegacy.element.ability.ProgressingAbility

class AirScythe(override val xpPerHit: Int = 1) : AirAbility(), ProgressingAbility {
    override val id: String
        get() = "al:air_scythe"

    override val name: String
        get() = "${element.toColorCode()}Air Scythe§r"

    override val cooldownTicks: Int
        get() = 5 // 0.25 sec cooldown
}