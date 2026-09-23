package net.kylanic.aangslegacy.commands

import net.kylanic.aangslegacy.bender.BenderManager
import net.kylanic.aangslegacy.element.ability.AbilityRegistry
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

class AbilityCommand : CommandExecutor, TabCompleter {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // /ability <slot> <move>
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /ability <slot: number> <move>.")
            return true
        }

        var moveIds = AbilityRegistry.getAllIds().toMutableList()
        moveIds.add("empty")

        var slot = args[0].toIntOrNull()
        if (slot == null) {
            sender.sendMessage("§cSlot must be a number, not ${args[0]}.")
            return true
        }

        if (slot !in 1..9) {
            sender.sendMessage("§cSlot must be a number between 1 and 9.")
            return true
        }

        slot -= 1

        val id: String = args[1]
        if (id !in moveIds) {
            sender.sendMessage("§cUnknown move id: $id, might be the wrong version, check the github page.")
            return true
        }

        if (sender !is Player) {
            sender.sendMessage("Only players can run this command.")
            return true
        }

        val bender = BenderManager.get(sender)
        if (bender == null) {
            sender.sendMessage("Player ${sender.name} is not a bender, somehow...")
            return true
        }

        val ability = AbilityRegistry.getAbilityDefinition(id)
        if (ability == null && id != "empty") {
            sender.sendMessage("Couldn't find ability in registry, somehow...")
            return true
        }

        if (id == "empty") {
            bender.abilityManager.setAbilitySlot(slot, null)
            sender.sendMessage("Emptied slot $slot")
        } else {
            bender.abilityManager.setAbilitySlot(slot, ability)
            sender.sendMessage("Set slot $slot to ${ability?.name}")
        }


        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String?>? {
        if (args.size == 1) {
            val nums = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
            val matches = mutableListOf<String>()
            StringUtil.copyPartialMatches(args[0], nums, matches)
            matches.sort()
            return matches
        }

        if (args.size == 2) {
            var moveIds = AbilityRegistry.getAllIds()
            val matches = mutableListOf<String>()
            StringUtil.copyPartialMatches(args[1], moveIds, matches)
            matches.sort()
            return matches
        }

        return emptyList()
    }

}