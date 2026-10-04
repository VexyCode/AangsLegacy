package net.kylanic.aangslegacy.element.progression

enum class AbilityLevel(
    val requiredXp: Int,
    val powerMultiplier: Double,
    val cooldownMultiplier: Double
) {
    LEARNED(0, 1.00, 1.00),
    PRACTICED(50, 1.10, 0.95),
    SKILLED(150, 1.20, 0.90),
    ADVANCED(350, 1.35, 0.85),
    EXPERT(750, 1.50, 0.80),
    MASTERED(1500, 1.75, 0.70);

    val next: AbilityLevel?
        get() = entries.getOrNull(ordinal + 1)

    val displayName: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }

    companion object {
        fun fromXP(xp: Int): AbilityLevel =
            entries.last { xp >= it.requiredXp }
    }
}
