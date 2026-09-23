package net.kylanic.aangslegacy.commands.test

import net.kylanic.aangslegacy.bender.BenderManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class TestGetAbilityCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.size < 2) {
            sender.sendMessage("Usage: /test_get_ability <player> <slot>")
            return true
        }

        val player = Bukkit.getPlayer(args[0])
        if (player == null) {
            sender.sendMessage("Player '${args[0]}' is not online or doesn't exist.")
            return true
        }

        val slot = args[1].toIntOrNull()
        if (slot == null) {
            sender.sendMessage("Slot must be a positive integer or 0.")
            return true
        }

        val bender = BenderManager.get(player)
        if (bender == null) {
            sender.sendMessage("Player ${player.name} is not a bender, somehow.")
            return true
        }

        val ability = bender.abilityManager.getAbilitySlot(slot)
        if (ability == null) {
            sender.sendMessage("Player has slot $slot empty.")
            return true
        }
        sender.sendMessage("Player has ability '${ability.name}' of element ${ability.element.toColorCode()}${ability.element.name}§r")
        return true
    }
}