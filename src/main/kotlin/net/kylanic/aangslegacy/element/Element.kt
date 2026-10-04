package net.kylanic.aangslegacy.element

enum class Element {
    Fire,
    Air,
    Water,
    Earth;

    fun toColorCode(): String = when (this) {
        Fire -> "§c"
        Air -> "§f"
        Water -> "§9"
        Earth -> "§a"
    }

    fun getNextInElementCycle(): Element = when (this) {
        Fire -> Air
        Air -> Water
        Water -> Earth
        Earth -> Fire
    }

    fun getPreviousElementCycle(): Element = when (this) {
        Fire -> Earth
        Air -> Fire
        Water -> Air
        Earth -> Water
    }

    companion object {
        fun fromString(string: String): Element? = when (string.lowercase()) {
            "fire" -> Fire
            "air" -> Air
            "water" -> Water
            "earth" -> Earth
            else -> null
        }
    }
}