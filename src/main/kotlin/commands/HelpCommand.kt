package commands

import Command
import Config.BuildInfo
import TwitchBotConfig
import commands

val helpCommand: Command = Command(
    names = listOf("help"),
    handler = {
        chat.sendMessage(TwitchBotConfig.channel,
            "Bot Version ${BuildInfo.version}. " +
                    "Available commands: " +
                    "${commands.joinToString("; ") { command -> command.names.joinToString("|") { "${TwitchBotConfig.commandPrefix}${it}" } }}."
        )
    }
)