package net.kylanic.aangslegacy.element.ability.fire

import net.kylanic.aangslegacy.element.ability.ProgressingAbility

class Fireball(override val xpPerHit: Int = 1) : FireAbility(), ProgressingAbility {
    override val id: String
        get() = "al:fireball"

    override val name: String
        get() = "${element.toColorCode()}Fireball§r"

    override val cooldownTicks: Int
        get() = 5  // 0.25 sec cooldown
}