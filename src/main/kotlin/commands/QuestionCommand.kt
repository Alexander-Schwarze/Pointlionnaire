package commands

import Command
import TwitchBotConfig
import handler.QuestionHandler
import kotlinx.datetime.Clock
import timestampNextAction
import kotlin.time.DurationUnit

val questionCommand: Command = Command(
    names = listOf("question", "q"),
    handler = {
        val currentTimeLeft = timestampNextAction.value?.minus(Clock.System.now())
        chat.sendMessage(
            TwitchBotConfig.channel,
            "Question is: ${QuestionHandler.instance?.currentQuestion?.value?.questionText.run {
                        "$this. " +   
                        if(this.equals(QuestionHandler.instance?.emptyQuestion?.questionText)) {
                            "Time until next question comes up: "
                        } else {
                            "Time until answer duration is over: "
                        }
                    }}" +
                    if(currentTimeLeft == null || currentTimeLeft.inWholeMilliseconds < 0) {
                        "No Timer Running"
                    } else {
                        currentTimeLeft.toString(DurationUnit.SECONDS, 0)
                    }
        )
    }
)