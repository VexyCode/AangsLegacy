package net.kylanic.aangslegacy.element.ability.air

class AirScythe : AirAbility() {
    override val id: String
        get() = "al:air_scythe"

    override val name: String
        get() = "${element.toColorCode()}Air Scythe§r"

    override val cooldownTicks: Int?
        get() = 5 // 0.25 sec cooldown
}