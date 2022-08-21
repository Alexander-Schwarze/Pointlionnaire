package commands

import Command
import handler.QuestionHandler
import handler.UserHandler
import java.util.*

val answerCommand: Command = Command(
    names = listOf("answer", "a"),
    handler = {
        val answer = it.joinToString(" ").lowercase(Locale.getDefault())

        if(QuestionHandler.instance?.currentQuestion?.value != QuestionHandler.instance?.emptyQuestion){
            if(UserHandler.isTieBreaker() && user !in UserHandler.tieBreakUsers){
                return@Command
            }

            if(QuestionHandler.instance?.checkAnswer(answer, user) == true) {
                QuestionHandler.instance.updateCurrentQuestionsLeaderboard(user)
            }
        }

    }
)