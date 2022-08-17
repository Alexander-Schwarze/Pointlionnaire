package commands

import Command
import User
import androidx.compose.ui.text.toLowerCase
import handler.QuestionHandler
import java.util.*

val answerCommand: Command = Command(
    names = listOf("answer", "a"),
    handler = {
        val answer = it.joinToString(" ").lowercase(Locale.getDefault())

        if(QuestionHandler.instance?.checkAnswer(answer, User(user.name, user.id)) == true && QuestionHandler.instance.currentQuestion.value != QuestionHandler.instance.emptyQuestion){
            QuestionHandler.instance.updateCurrentQuestionsLeaderboard(User(user.name, user.id))
        }

    }
)