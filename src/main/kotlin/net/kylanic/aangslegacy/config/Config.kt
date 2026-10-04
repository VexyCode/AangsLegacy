package net.kylanic.aangslegacy.config

import net.kylanic.aangslegacy.AangsLegacy
import net.kylanic.aangslegacy.bender.Bender
import net.kylanic.aangslegacy.element.Element
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.UUID

object Config {

    fun saveBender(bender: Bender): YamlConfiguration {
        val playerFile = File(
            AangsLegacy.dataFolder,
            "players/${bender.id}.yaml"
        )

        if (!playerFile.exists()) {
            playerFile.parentFile.mkdirs()
            playerFile.createNewFile()
        }

        val player = YamlConfiguration.loadConfiguration(playerFile)

        bender.save(player)

        player.save(playerFile)

        AangsLegacy.logger.info("Saved bender ${bender.id}")

        return player
    }

    fun loadBender(benderFileName: String): Bender? {
        val playerFile = File(
            AangsLegacy.dataFolder,
            "players/$benderFileName"
        )

        if (!playerFile.exists()) {
            AangsLegacy.logger.warning(
                "Bender file '$benderFileName' doesn't exist. Skipping."
            )
            return null
        }

        val player = YamlConfiguration.loadConfiguration(playerFile)

        val uuid = try {
            UUID.fromString(benderFileName.removeSuffix(".yaml"))
        } catch (_: IllegalArgumentException) {
            AangsLegacy.logger.warning(
                "Invalid bender UUID '$benderFileName'. Skipping."
            )
            return null
        }

        val elementString = player.getString("native_element")

        val element = if (elementString == null) {
            AangsLegacy.logger.warning(
                "Bender '$uuid' does not have a native element. " +
                        "Defaulting to Fire."
            )
            Element.Fire
        } else {
            Element.fromString(elementString) ?: run {
                AangsLegacy.logger.warning(
                    "Unknown element '$elementString'. Defaulting to Fire."
                )
                Element.Fire
            }
        }

        val abilityIds = MutableList<String?>(9) { slot ->
            player.getString("abilities.$slot")
        }

        val bender = Bender(
            id = uuid,
            nativeElement = element,
            unlockedElements = mutableListOf(),
            abilityIds = abilityIds
        )

        val attunements = player.getConfigurationSection("element_attunements")

        if (attunements != null) {
            for (elementName in attunements.getKeys(false)) {
                val loadedElement = Element.fromString(elementName) ?: continue
                val value = attunements.getInt(elementName)

                bender.elementAttunement[loadedElement] = value

                if (value >= 3) {
                    bender.unlockedElements.add(loadedElement)
                }
            }
        }

        bender.progression.load(player)

        val usedTemples = player.getList("used_temple_locations")

        if (usedTemples != null) {
            for (entry in usedTemples) {
                if (entry is Location) {
                    bender.usedTemples.add(entry)
                }
            }
        }

        return bender
    }

    fun loadBenders(benderFileNames: List<String>): Map<UUID, Bender> {
        val benders = mutableMapOf<UUID, Bender>()

        for (fileName in benderFileNames) {
            val bender = loadBender(fileName)

            if (bender == null) {
                AangsLegacy.logger.warning(
                    "Bender for file $fileName is null. Skipping."
                )
                continue
            }

            benders[bender.id] = bender
        }

        return benders
    }

    fun loadDefaults(plugin: AangsLegacy) {
        val res = plugin.javaClass.getResource("/config")

        if (res == null) {
            plugin.logger.warning(
                "Could not find /defaults in plugin resources."
            )
            return
        }

        val uri = res.toURI()

        if (uri.scheme != "jar") {
            plugin.logger.warning(
                "/defaults is not inside jar. (${uri.scheme})"
            )
            return
        }

        val fs = try {
            FileSystems.getFileSystem(uri)
        } catch (_: FileSystemNotFoundException) {
            FileSystems.newFileSystem(
                uri,
                emptyMap<String, Any>()
            )
        }

        val defaults = fs.getPath("/config")

        Files.walk(defaults).use { paths ->
            paths
                .filter { Files.isRegularFile(it) }
                .forEach { path ->
                    val relative = defaults.relativize(path).toString()

                    val destination = File(
                        plugin.dataFolder,
                        "config/$relative"
                    )

                    if (!destination.exists()) {
                        destination.parentFile.mkdirs()

                        plugin.saveResource(
                            "config/$relative",
                            false
                        )
                    }
                }
        }
    }

    fun get(configName: String): YamlConfiguration? {
        val file = File(
            AangsLegacy.dataFolder,
            configName
        )

        if (!file.exists()) {
            return null
        }

        return YamlConfiguration.loadConfiguration(file)
    }

    fun getEarthbendableBlocks(): List<Material> {
        val earthbendables = get("config/earthbendable_blocks.yaml")

        if (earthbendables == null) {
            AangsLegacy.logger.warning(
                "No earthbendable block list found, " +
                        "Earthbending moves might not work correctly " +
                        "or not work at all."
            )
            return emptyList()
        }

        val blocks = earthbendables.getStringList("blocks")

        if (blocks.isEmpty()) {
            AangsLegacy.logger.warning(
                "No earthbendable blocks in config file " +
                        "or block entry not found."
            )
            return emptyList()
        }

        val materials = mutableListOf<Material>()

        for (block in blocks) {
            val material = Material.matchMaterial(block)

            if (material == null) {
                AangsLegacy.logger.warning(
                    "Unknown block type: $block."
                )
                continue
            }

            materials.add(material)
        }

        return materials
    }
}