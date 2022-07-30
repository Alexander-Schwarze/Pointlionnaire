package commands

import Command
import TwitchBotConfig
import commands

val helpCommand: Command = Command(
    names = listOf("help"),
    handler = {
        chat.sendMessage(TwitchBotConfig.channel, "Available commands: ${commands.joinToString("; ") { command -> command.names.joinToString("|") { "${TwitchBotConfig.commandPrefix}${it}" } }}.")
    }
)