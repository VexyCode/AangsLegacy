package net.kylanic.aangslegacy.commands

import net.kylanic.aangslegacy.bender.Bender
import net.kylanic.aangslegacy.bender.BenderManager
import net.kylanic.aangslegacy.element.Element
import net.kylanic.aangslegacy.element.ability.Ability
import net.kylanic.aangslegacy.element.ability.AbilityRegistry
import net.kylanic.aangslegacy.element.ability.ProgressingAbility
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.Locale
import kotlin.math.roundToInt

class ElementCommand : CommandExecutor, TabCompleter {

    private companion object {
        const val PERM_ADMIN = "bending.command.element.admin"
        val SUBCOMMANDS = listOf("info", "who", "ability", "attunement", "add", "remove")
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
            "who" -> handleWho(sender, label, args)
            "ability" -> handleAbility(sender, label, args)
            "add" -> handleModify(sender, label, args, unlock = true)
            "remove" -> handleModify(sender, label, args, unlock = false)
            "attunement" -> handleAttunement(sender, label, args)
            else -> usage(sender, "/$label <info|who|ability|attunement|add|remove> ...")
        }
    }


    private fun handleInfo(sender: CommandSender, targetName: String?): Boolean {
        val target = resolveTarget(sender, targetName, "info") ?: return true
        val bender = BenderManager.get(target) ?: return noBender(sender, target)

        sender.sendMessage(bender.info(target.name))
        if (target != sender) target.sendMessage("Somebody checked you out today...")
        return true
    }

    private fun handleWho(sender: CommandSender, label: String, args: Array<out String>): Boolean {
        if (args.size != 2) return usage(sender, "/$label who <player>")

        val target = resolveTarget(sender, args[1], "who") ?: return true
        val bender = BenderManager.get(target) ?: return noBender(sender, target)

        sender.sendMessage("${target.name} is a/an ${bender.nativeElement}bender.")
        return true
    }


    private fun handleAbility(sender: CommandSender, label: String, args: Array<out String>): Boolean {
        // args[0] == "ability"
        if (args.size < 2) {
            return usage(sender, "/$label ability <info [ability] | [slot] <move|empty>>")
        }

        return if (args[1].equals("info", ignoreCase = true)) {
            handleAbilityInfo(sender, label, args)
        } else {
            handleAbilitySet(sender, label, args)
        }
    }

    private fun handleAbilitySet(sender: CommandSender, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can bind abilities. Consoles don't have hotbars.")
            return true
        }

        val bender = BenderManager.get(sender) ?: return noBender(sender, sender)
        val moveIds = AbilityRegistry.getIdsForElements(bender.unlockedElements)

        val slot: Int
        val rawId: String

        when (args.size) {
            2 -> { // /element ability <move>  -> held slot
                slot = sender.inventory.heldItemSlot
                rawId = args[1]
            }
            3 -> { // /element ability <slot> <move>
                val parsed = args[1].toIntOrNull() ?: run {
                    sender.sendMessage("§cSlot must be a number, not ${args[1]}.")
                    return true
                }
                if (parsed !in 1..9) {
                    sender.sendMessage("§cSlot must be a number between 1 and 9.")
                    return true
                }
                slot = parsed - 1
                rawId = args[2]
            }
            else -> return usage(sender, "/$label ability [slot] <move|empty>")
        }

        if (rawId.equals("empty", ignoreCase = true)) {
            bender.abilityManager.setAbilitySlot(slot, null)
            sender.sendMessage("Emptied slot ${slot + 1}.")
            return true
        }

        val id = resolveAbilityId(rawId, moveIds) ?: run {
            sender.sendMessage("§cUnknown move: $rawId. Either it's not real or you can't bend that element yet.")
            return true
        }

        val ability = AbilityRegistry.getAbilityDefinition(id) ?: run {
            sender.sendMessage("Couldn't find ability in registry, somehow...")
            return true
        }

        bender.abilityManager.setAbilitySlot(slot, ability)
        sender.sendMessage("Set slot ${slot + 1} to ${ability.name}")
        return true
    }

    private fun handleAbilityInfo(sender: CommandSender, label: String, args: Array<out String>): Boolean {
        if (args.size > 3) return usage(sender, "/$label ability info [ability]")

        if (sender !is Player) {
            sender.sendMessage("Only players have abilities to look at.")
            return true
        }

        val bender = BenderManager.get(sender) ?: return noBender(sender, sender)
        val available = AbilityRegistry.getIdsForElements(bender.unlockedElements)

        // /element ability info <ability>  -> the full breakdown
        if (args.size == 3) {
            val id = resolveAbilityId(args[2], available) ?: run {
                sender.sendMessage("§cUnknown move: ${args[2]}. Either it's not real or you can't bend that element yet.")
                return true
            }
            val ability = AbilityRegistry.getAbilityDefinition(id) ?: return true

            sender.sendMessage(abilityDetails(ability, bender))
            return true
        }

        // /element ability info  -> one line per ability
        if (available.isEmpty()) {
            sender.sendMessage("You don't have any abilities yet.")
            return true
        }

        val lines = available
            .mapNotNull { AbilityRegistry.getAbilityDefinition(it) }
            .joinToString("\n") { abilityLine(it, bender) }

        sender.sendMessage("§6Your abilities:\n$lines")
        return true
    }

    private fun abilityLine(ability: Ability, bender: Bender): String {
        if (ability as? ProgressingAbility == null) {
            return " - ${ability.name} §7(doesn't level up)"
        }

        val progression = bender.progression
        val level = progression.getLevel(ability.id)
        val xp = progression.getXp(ability.id)
        val toNext = progression.xpToNextLevel(ability.id)
        val next = level.next

        val progress = if (toNext != null && next != null)
            "$xp XP, $toNext to ${next.displayName}"
        else
            "$xp XP, max level"

        return " - ${ability.name} §7- §e${level.displayName} §7($progress)"
    }

    private fun abilityDetails(ability: Ability, bender: Bender): String {
        val progressing = ability as? ProgressingAbility
            ?: return "${ability.name}\n§7This ability doesn't level up.\n" +
                    "§7Cooldown: §f${seconds(ability.cooldownTicks)}s"

        val progression = bender.progression
        val level = progression.getLevel(ability.id)
        val xp = progression.getXp(ability.id)
        val toNext = progression.xpToNextLevel(ability.id)
        val next = level.next

        val effectiveCooldown = (ability.cooldownTicks * level.cooldownMultiplier)
            .roundToInt()
            .coerceAtLeast(1)

        val xpLine = if (toNext != null && next != null)
            "$xp XP §7($toNext more for §e${next.displayName}§7)"
        else
            "$xp XP §7(max level)"

        return buildString {
            appendLine("${ability.name} §7[§e${level.displayName}§7]")
            appendLine("§7XP: §f$xpLine")
            appendLine("§7Power: §fx${"%.2f".format(Locale.ROOT, level.powerMultiplier)}")
            appendLine(
                "§7Cooldown: §f${seconds(effectiveCooldown)}s " +
                        "§7(base ${seconds(ability.cooldownTicks)}s)"
            )
            append("§7XP per hit: §f${progressing.xpPerHit}")
        }
    }

    private fun resolveAbilityId(raw: String, availableIds: List<String>): String? {
        val lowered = raw.lowercase()
        if (lowered in availableIds) return lowered

        val prefixed = "al:$lowered"
        return if (prefixed in availableIds) prefixed else null
    }

    private fun seconds(ticks: Int): String =
        "%.2f".format(Locale.ROOT, ticks / 20.0).trimEnd('0').trimEnd('.')


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


    /** name == null -> the sender (must be a Player). Otherwise look the player up. Sends the error itself. */
    private fun resolveTarget(sender: CommandSender, name: String?, what: String): Player? {
        if (name == null) {
            if (sender !is Player) {
                sender.sendMessage("You gotta be a player to run '$what' on yourself. Try adding a player or smth, don't ask me!")
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


    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        val elements = Element.entries.map { it.name.lowercase() }
        val players = Bukkit.getOnlinePlayers().map { it.name }
        val isAdmin = sender.hasPermission(PERM_ADMIN)
        val current = args.last()

        // abilities this player can actually use, both "al:fireball" and "fireball"
        val abilityOptions: List<String> = (sender as? Player)
            ?.let { BenderManager.get(it) }
            ?.let { AbilityRegistry.getIdsForElements(it.unlockedElements) }
            ?.let { ids -> ids + ids.map { it.removePrefix("al:") } }
            ?: emptyList()

        val slots = (1..9).map { it.toString() }

        val options: List<String> = when (args.size) {
            1 -> SUBCOMMANDS.filter { isAdmin || (it != "add" && it != "remove") }

            2 -> when (args[0].lowercase()) {
                "info", "who" -> players
                "ability" -> listOf("info", "empty") + slots + abilityOptions
                "add", "remove" -> if (isAdmin) elements + players else emptyList()
                "attunement" -> listOf("get")
                else -> emptyList()
            }

            3 -> when (args[0].lowercase()) {
                "ability" -> when {
                    args[1].equals("info", ignoreCase = true) -> abilityOptions
                    args[1].toIntOrNull() != null -> listOf("empty") + abilityOptions
                    else -> emptyList()
                }
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