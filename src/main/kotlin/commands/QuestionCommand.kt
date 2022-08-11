package commands

import Command
import TwitchBotConfig
import handler.QuestionHandler

val questionCommand: Command = Command(
    names = listOf("question", "q"),
    handler = {
        // TODO: Make command maybe display time that is left?
        chat.sendMessage(TwitchBotConfig.channel, "Question is: ${QuestionHandler.instance?.currentQuestion?.value?.questionText}")
    }
)