package commands

import Command
import handler.QuestionHandler
import handler.UserHandler
import java.util.*

val answerCommand = Command(
    names = listOf("answer", "a"),
    handler = {
        val answer = it.joinToString(" ").lowercase(Locale.getDefault())

        if (QuestionHandler.instance.currentQuestion.value != null) {
            if (UserHandler.isTieBreaker && user !in UserHandler.getTieBreakerUsers()) {
                return@Command
            }

            if (QuestionHandler.instance.checkAnswer(answer, user)) {
                QuestionHandler.instance.updateCurrentQuestionsLeaderboard(user)
            }
        }
    }
)