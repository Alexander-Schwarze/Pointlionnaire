package commands

import Command
import QuestionHandler
import TwitchBotConfig

val questionCommand: Command = Command(
    names = listOf("question", "q"),
    handler = {
        // TODO: Make command display current pending question (and maybe time that is left?)
        // WHY IS THIS NOT WORKING WTF
        chat.sendMessage(TwitchBotConfig.channel, "Question is: ${QuestionHandler.instance}")
    }
)