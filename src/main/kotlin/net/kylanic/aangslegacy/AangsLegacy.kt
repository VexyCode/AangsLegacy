package net.kylanic.aangslegacy

import net.kylanic.aangslegacy.commands.WhoCommand
import net.kylanic.aangslegacy.listener.LoginListener
import net.kylanic.aangslegacy.bender.BenderManager
import net.kylanic.aangslegacy.commands.AbilityCommand
import net.kylanic.aangslegacy.element.abilityinstance.InstanceManager
import net.kylanic.aangslegacy.event.EventManager
import net.kylanic.aangslegacy.listener.ArmSwingListener
import net.kylanic.aangslegacy.listener.HotbarSlotChangeListener
import net.kylanic.aangslegacy.listener.ShiftLookListener
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.logging.Logger
import org.bukkit.event.Listener


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

        Companion.logger.info("AangsLegacy Loaded! Let's Bend!")

        registerListeners(
            LoginListener(),
            HotbarSlotChangeListener(),
            ArmSwingListener(),
            ShiftLookListener(),
        )

        BenderManager.load()
        registerCommands(
            "who" to WhoCommand(),
            "ability" to AbilityCommand(),
        )

        registerSchedulerFunctions(
            Runnable { InstanceManager.tickAllAbilityInstances() } to 1L,
            Runnable { EventManager.handleEvents() } to 1L,
            Runnable { BenderManager.tickCooldowns() } to 1L,
        )
    }

    override fun onDisable() {
        BenderManager.save()
    }

    fun registerListeners(vararg listeners: Listener) {
        for (listener in listeners) server.pluginManager.registerEvents(listener, this)
    }

    fun registerCommands(vararg commands: Pair<String, CommandExecutor>) {
        for ((id, command) in commands) {
            getCommand(id)?.setExecutor(command)
            if (command is TabCompleter)
                getCommand(id)?.tabCompleter = command
        }
    }

    fun registerSchedulerFunctions(vararg fns: Pair<Runnable, Long>) {
        for ((fn, period) in fns) {
            server.scheduler.runTaskTimer(
                this,
                fn,
                0L,
                period
            )
        }
    }
}
