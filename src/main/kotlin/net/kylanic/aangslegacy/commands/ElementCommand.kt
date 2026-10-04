package net.kylanic.aangslegacy.commands

import net.kylanic.aangslegacy.bender.BenderManager
import net.kylanic.aangslegacy.element.Element
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class ElementCommand : CommandExecutor, TabCompleter {

    private companion object {
        const val PERM_ADMIN = "bending.command.element.admin" // add/remove. Change or delete to taste.
        val SUBCOMMANDS = listOf("info", "add", "remove", "attunement")
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) return handleInfo(sender, null)

        return when (args[0].lowercase()) {
            "info" -> {
                if (args.size > 2) usage(sender, "/$label info [player]")
                else handleInfo(sender, args.getOrNull(1))
            }
            "add" -> handleModify(sender, label, args, unlock = true)
            "remove" -> handleModify(sender, label, args, unlock = false)
            "attunement" -> handleAttunement(sender, label, args)
            else -> usage(sender, "/$label <info|add|remove|attunement> ...")
        }
    }

    private fun handleInfo(sender: CommandSender, targetName: String?): Boolean {
        val target = resolveTarget(sender, targetName, "info") ?: return true
        val bender = BenderManager.get(target) ?: return noBender(sender, target)

        sender.sendMessage(bender.info(target.name))
        if (target != sender) target.sendMessage("Somebody checked you out today...")
        return true
    }

    private fun handleModify(
        sender: CommandSender,
        label: String,
        args: Array<out String>,
        unlock: Boolean
    ): Boolean {
        val verb = if (unlock) "add" else "remove"

        if (!sender.hasPermission(PERM_ADMIN)) {
            sender.sendMessage("You can't do that. Ask somebody with more power than you.")
            return true
        }

        // '/element add <element>' or '/element add <player> <element>'
        val (targetName, elementName) = when (args.size) {
            2 -> null to args[1]
            3 -> args[1] to args[2]
            else -> return usage(sender, "/$label $verb [player] <element>")
        }

        val target = resolveTarget(sender, targetName, verb) ?: return true

        val element = Element.fromString(elementName) ?: run {
            sender.sendMessage("What element do you think '$elementName' is, it's not in the plugin, that's for sure... (disappointed in binary)")
            return true
        }

        val bender = BenderManager.get(target) ?: return noBender(sender, target)

        if (unlock) bender.unlockElement(element, target) else bender.lockElement(element, target)
        return true
    }

    private fun handleAttunement(sender: CommandSender, label: String, args: Array<out String>): Boolean {
        // args[0] == "attunement"
        if (args.size < 2 || !args[1].equals("get", ignoreCase = true)) {
            return usage(sender, "/$label attunement get [element|player] [element]")
        }

        when (args.size) {
            2 -> { // '/element attunement get'
                val target = resolveTarget(sender, null, "attunement get") ?: return true
                val bender = BenderManager.get(target) ?: return noBender(sender, target)
                sender.sendMessage(bender.getAttunementInfo())
            }

            3 -> { // '/element attunement get <element>' or '/element attunement get <player>'
                val lastArg = args[2]
                val element = Element.fromString(lastArg)

                if (element != null) {
                    val target = resolveTarget(sender, null, "attunement get") ?: return true
                    val bender = BenderManager.get(target) ?: return noBender(sender, target)
                    sender.sendMessage(bender.getAttunementInfo(element))
                } else {
                    val target = Bukkit.getPlayer(lastArg) ?: run {
                        sender.sendMessage("Unknown player or element '$lastArg'. Try again, ig...")
                        return true
                    }
                    val bender = BenderManager.get(target) ?: return noBender(sender, target)
                    sender.sendMessage(bender.getAttunementInfo())
                }
            }

            4 -> { // '/element attunement get <player> <element>'
                val target = resolveTarget(sender, args[2], "attunement get") ?: return true

                val element = Element.fromString(args[3]) ?: run {
                    sender.sendMessage("What element do you think '${args[3]}' is, it's not in the plugin, that's for sure... (disappointed in binary)")
                    return true
                }

                val bender = BenderManager.get(target) ?: return noBender(sender, target)
                sender.sendMessage(bender.getAttunementInfo(element))
            }

            else -> return usage(sender, "/$label attunement get [element|player] [element]")
        }
        return true
    }

    // ------------------------------------------------------------------ helpers

    /** name == null -> the sender (must be a Player). Otherwise look the player up. Sends the error itself. */
    private fun resolveTarget(sender: CommandSender, name: String?, what: String): Player? {
        if (name == null) {
            if (sender !is Player) {
                sender.sendMessage("You gotta be a player to run /element $what on yourself. Try adding a player or smth, don't ask me!")
                return null
            }
            return sender
        }
        return Bukkit.getPlayer(name) ?: run {
            sender.sendMessage("Player '$name' either isn't online or doesn't exist. That is for you to figure out.")
            null
        }
    }

    private fun noBender(sender: CommandSender, target: Player): Boolean {
        sender.sendMessage("There is no bender attached to ${target.name} (id ${target.uniqueId}).")
        return true
    }

    private fun usage(sender: CommandSender, text: String): Boolean {
        sender.sendMessage("Usage: $text")
        return true
    }

    // ------------------------------------------------------------------ tab complete

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        val elements = Element.values().map { it.name.lowercase() }
        val players = Bukkit.getOnlinePlayers().map { it.name }
        val isAdmin = sender.hasPermission(PERM_ADMIN)
        val current = args.last()

        val options: List<String> = when (args.size) {
            1 -> SUBCOMMANDS.filter { isAdmin || (it != "add" && it != "remove") }

            2 -> when (args[0].lowercase()) {
                "info" -> players
                "add", "remove" -> if (isAdmin) elements + players else emptyList()
                "attunement" -> listOf("get")
                else -> emptyList()
            }

            3 -> when (args[0].lowercase()) {
                "add", "remove" ->
                    if (isAdmin && Element.fromString(args[1]) == null) elements else emptyList()
                "attunement" ->
                    if (args[1].equals("get", ignoreCase = true)) elements + players else emptyList()
                else -> emptyList()
            }

            4 -> when {
                args[0].equals("attunement", ignoreCase = true) &&
                        args[1].equals("get", ignoreCase = true) &&
                        Element.fromString(args[2]) == null -> elements // args[2] was a player
                else -> emptyList()
            }

            else -> emptyList()
        }

        return options
            .filter { it.startsWith(current, ignoreCase = true) }
            .distinct()
            .sorted()
    }
}