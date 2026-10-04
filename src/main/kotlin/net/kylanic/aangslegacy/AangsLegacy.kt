package net.kylanic.aangslegacy

import net.kylanic.aangslegacy.bender.BenderManager
import net.kylanic.aangslegacy.commands.AbilityCommand
import net.kylanic.aangslegacy.commands.ElementCommand
import net.kylanic.aangslegacy.commands.WhoCommand
import net.kylanic.aangslegacy.config.Config
import net.kylanic.aangslegacy.element.abilityinstance.InstanceManager
import net.kylanic.aangslegacy.event.EventManager
import net.kylanic.aangslegacy.listener.ArmSwingListener
import net.kylanic.aangslegacy.listener.BellRingListener
import net.kylanic.aangslegacy.listener.HotbarSlotChangeListener
import net.kylanic.aangslegacy.listener.LoginListener
import net.kylanic.aangslegacy.listener.ShiftLookListener
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.event.Listener
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

    override fun onLoad() {
        AangsLegacy.dataFolder = super.dataFolder
        AangsLegacy.logger = super.logger

        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        Config.loadDefaults(this)
    }

    override fun onEnable() {
        logger.info(
            "AangsLegacy Loaded! Let's Bend!"
        )

        registerListeners(
            LoginListener(),
            HotbarSlotChangeListener(),
            ArmSwingListener(),
            ShiftLookListener(),
            BellRingListener(),
        )

        BenderManager.load()

        registerCommands(
            "who" to WhoCommand(),
            "ability" to AbilityCommand(),
            "element" to ElementCommand(),
        )

        registerSchedulerFunctions(

            Runnable {
                InstanceManager.tickAllAbilityInstances()
            } to 1L,

            Runnable {
                EventManager.handleEvents()
            } to 1L,

            Runnable {
                BenderManager.tickCooldowns()
            } to 1L
        )
    }

    override fun onDisable() {
        BenderManager.save()
    }

    fun registerListeners(
        vararg listeners: Listener
    ) {

        for (listener in listeners) {

            server.pluginManager.registerEvents(
                listener,
                this
            )
        }
    }

    fun registerCommands(vararg commands: Pair<String, CommandExecutor>) {
        for ((id, executor) in commands) {
            val cmd = object : Command(id) {
                override fun execute(sender: CommandSender, label: String, args: Array<out String>): Boolean =
                    executor.onCommand(sender, this, label, arrayOf(*args))

                override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String> =
                    (executor as? TabCompleter)?.onTabComplete(sender, this, alias, arrayOf(*args)) ?: emptyList()
            }
            server.commandMap.register("aangslegacy", cmd)
        }
    }

    fun registerSchedulerFunctions(
        vararg fns: Pair<Runnable, Long>
    ) {

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
