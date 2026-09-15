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