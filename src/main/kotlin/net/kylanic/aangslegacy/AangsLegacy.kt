package net.kylanic.aangslegacy

import net.kylanic.aangslegacy.commands.WhoCommand
import net.kylanic.aangslegacy.listener.LoginListener
import net.kylanic.aangslegacy.player.BenderManager
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.logging.Logger

class AangsLegacy : JavaPlugin() {
    companion object {
        lateinit var dataFolder: File
            private set
        lateinit var logger: Logger
            private set
    }

    override fun onEnable() {
        AangsLegacy.dataFolder = super.dataFolder
        AangsLegacy.logger = super.logger

        if (!dataFolder.exists()) dataFolder.mkdirs()

        Companion.logger.info("Aangs Legacy Loaded! Let's Bend!")

        server.pluginManager.registerEvents(
            LoginListener(),
            this
        )

        BenderManager.load()
        getCommand("who")?.setExecutor(WhoCommand())
    }

    override fun onDisable() {
        BenderManager.save()
    }
}
