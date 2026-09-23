package net.kylanic.aangslegacy.commands

import net.kylanic.aangslegacy.bender.BenderManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.util.StringUtil

class WhoCommand : CommandExecutor, TabCompleter {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("Usage: /who <player>")
            return true
        }

        val player = Bukkit.getPlayer(args[0])
        if (player == null) {
            sender.sendMessage("Player '${args[0]}' is not online or doesn't exist.")
            return true
        }

        val bender = BenderManager.get(player)
        if (bender == null) {
            sender.sendMessage("Player ${player.name} is not a bender, somehow.")
            return true
        }

        sender.sendMessage("${player.name} is a/an ${bender.nativeElement}bender.")
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (args.size == 1) {
            val onlineNames = Bukkit.getOnlinePlayers().map { it.name }
            val matches = mutableListOf<String>()
            StringUtil.copyPartialMatches(args[0], onlineNames, matches)
            matches.sort()
            return matches
        }
        return emptyList()
    }
}