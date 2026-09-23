package net.kylanic.aangslegacy.util

import org.bukkit.Location
import org.bukkit.block.Biome

object BiomeUtils {
    val coldBiomes = setOf(
        Biome.COLD_OCEAN,
        Biome.DEEP_COLD_OCEAN,
        Biome.DEEP_FROZEN_OCEAN,
        Biome.FROZEN_OCEAN,
        Biome.FROZEN_PEAKS,
        Biome.FROZEN_RIVER,
        Biome.GROVE,
        Biome.ICE_SPIKES,
        Biome.JAGGED_PEAKS,
        Biome.SNOWY_BEACH,
        Biome.SNOWY_PLAINS,
        Biome.SNOWY_SLOPES,
        Biome.SNOWY_TAIGA
    )

    fun isCold(location: Location): Boolean =
        location.world.getBiome(location) in coldBiomes
}