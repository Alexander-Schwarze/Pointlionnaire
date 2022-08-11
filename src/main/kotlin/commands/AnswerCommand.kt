package commands

import Command
import User
import handler.QuestionHandler

val answerCommand: Command = Command(
    names = listOf("answer", "a"),
    handler = {
        val answer = it.joinToString()

        if(QuestionHandler.instance?.checkAnswer(answer) == true && QuestionHandler.instance.currentQuestion.value != QuestionHandler.instance.emptyQuestion){
            QuestionHandler.instance.updateCurrentQuestionsLeaderboard(User(user.name, user.id))
        }

    }
)