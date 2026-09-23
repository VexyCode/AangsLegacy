package net.kylanic.aangslegacy.element.ability.water

class WaterSplash : WaterAbility() {
    override val id: String
        get() = "al:water_splash"

    override val name: String
        get() = "${element.toColorCode()}Water Splash§r"

    override val cooldownTicks: Int?
        get() = 4 * 20  // 4 seconds on a normal tps.
}