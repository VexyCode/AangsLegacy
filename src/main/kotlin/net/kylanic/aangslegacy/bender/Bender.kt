package net.kylanic.aangslegacy.player

import net.kylanic.aangslegacy.AangsLegacy
import net.kylanic.aangslegacy.element.Element
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID

class Bender(
    val id: UUID,
    val nativeElement: Element
) {
    fun save(players: YamlConfiguration): Unit {
        players.set("${id}.element", nativeElement.name)
    }
}