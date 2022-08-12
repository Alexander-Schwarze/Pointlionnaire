package handler

import Question
import TwitchBotConfig
import User
import json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.decodeFromString
import logger
import java.io.File

class QuestionHandler private constructor(
    private val questions: Set<Question>
) {
    companion object {
        val instance = run {
            val questionsFile = File("data/questions.json")

            val questions = if (!questionsFile.exists()) {
                questionsFile.createNewFile()
                logger.info("Questions file created.")
                setOf()
            } else {
                try {
                    json.decodeFromString<Set<Question>>(questionsFile.readText()).filter {
                        it.id >= 0
                    }.toSet().also { currentQuestionsData ->
                        logger.info("Existing questions file found! Values: ${currentQuestionsData.joinToString(" | ")}")
                    }
                } catch (e: Exception) {
                    logger.warn("Error while reading questions file. Initializing empty questions", e)
                    setOf()
                }
            }

            if(questions.isEmpty()){
                // TODO: Remove this as soon as questions can be added via UI
                logger.error("There are no existing questions. As for version 1.0.0, you cannot set questions in the UI. Thus they need to be added before app start in the json-file.")
                return@run null
            }

            QuestionHandler(questions)
        }
    }

    val emptyQuestion = Question(id = -1, questionText = TwitchBotConfig.noQuestionPendingText, answer = "", isLast2Questions = false, isTieBreakerQuestion = false)
    val currentQuestion = MutableStateFlow(emptyQuestion)

    private val askedQuestions = mutableMapOf<Question, /* leader board: */ List<User>>()
    private val askedTieBreakerQuestions = mutableMapOf<Question, /* winner: */ User?>()

    fun popRandomQuestion(isLast2Questions: Boolean): Question {
        return if(isLast2Questions) {
            questions.filter{
                it.isLast2Questions && it !in askedQuestions && !it.isTieBreakerQuestion
            }
        } else {
            questions.filter{
                !it.isLast2Questions && it !in askedQuestions && !it.isTieBreakerQuestion
            }
        }.random().also {
            askedQuestions[it] = listOf()
            currentQuestion.value = it
        }
    }

    fun popRandomTieBreakerQuestion(): Question {
        return questions.filter {
            it.isTieBreakerQuestion && it !in askedTieBreakerQuestions
        }.random().also {
            askedTieBreakerQuestions[it] = null
        }
    }

    fun updateCurrentQuestionsLeaderboard(user: User) {
        val newLeaderboard = (askedQuestions[currentQuestion.value] ?: listOf()).toMutableList()

        if(newLeaderboard.size == 3) {
            return
        }

        newLeaderboard += user
        askedQuestions[currentQuestion.value] = newLeaderboard
    }

    fun getCurrentLeaderboard(): List<User> {
        return askedQuestions[currentQuestion.value] ?: listOf()
    }

    fun checkAnswer(answer: String): Boolean {
        return answer.trim() == currentQuestion.value.answer
    }

    fun resetCurrentQuestion() {
        currentQuestion.value = emptyQuestion
    }
}