package commands

import Command

val answerCommand: Command = Command(
    names = listOf("answer", "a"),
    handler = {
        // TODO: Implement answer
        chat.sendMessage(TwitchBotConfig.channel, "")
    }
)