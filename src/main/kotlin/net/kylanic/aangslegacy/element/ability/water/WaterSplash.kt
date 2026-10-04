package net.kylanic.aangslegacy.element.ability.water

import net.kylanic.aangslegacy.element.ability.ProgressingAbility

class WaterSplash(override val xpPerHit: Int = 8) : WaterAbility(), ProgressingAbility {
    override val id: String
        get() = "al:water_splash"

    override val name: String
        get() = "${element.toColorCode()}Water Splash§r"

    override val cooldownTicks: Int
        get() = 4 * 20  // 4 seconds on a normal tps.
}