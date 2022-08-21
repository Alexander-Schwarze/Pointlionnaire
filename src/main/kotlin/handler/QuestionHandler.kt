package handler

import Question
import TwitchBotConfig
import TwitchBotConfig.maxAmountTries
import com.github.twitch4j.common.events.domain.EventUser
import json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.decodeFromString
import logger
import java.io.File
import java.util.*

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
                    }.onEach {
                        it.answer = it.answer.lowercase(Locale.getDefault())
                    }.toSet().also { currentQuestionsData ->
                        logger.info("Existing questions file found! Values: ${currentQuestionsData.joinToString(" | ")}")
                    }
                } catch (e: Exception) {
                    logger.warn("Error while reading questions file. Initializing empty questions", e)
                    setOf()
                }
            }

            if(
                questions.filter { !it.isTieBreakerQuestion }.size < TwitchBotConfig.amountQuestions ||
                questions.filter { !it.isLast2Questions && !it.isTieBreakerQuestion }.size < TwitchBotConfig.amountQuestions - 2 ||
                questions.filter { it.isLast2Questions && !it.isTieBreakerQuestion }.size < 2 ||
                questions.none { it.isTieBreakerQuestion }
            ){
                // TODO: Remove this as soon as questions can be added via UI
                logger.error("There was an error with given questions. As for version 1.0.0, you cannot set questions in the UI. Thus they need to be added before app start in the json-file.")
                logger.info("You need: " +
                        "At least ${TwitchBotConfig.amountQuestions} total questions who are no tiebreaker questions. " +
                        "At least ${TwitchBotConfig.amountQuestions - 2} questions which are neither tiebreaker nor last 2 questions. " +
                        "At least 2 questions which are last 2 questions and no tiebreaker. " +
                        "At least 1 tiebreaker question."
                )
                return@run null
            }

            QuestionHandler(questions)
        }
    }

    val emptyQuestion = Question(id = -1, questionText = TwitchBotConfig.noQuestionPendingText, answer = "None", isLast2Questions = false, isTieBreakerQuestion = false)
    val currentQuestion = MutableStateFlow(emptyQuestion)

    val askedQuestions = mutableMapOf<Question, /* leader board: */ List<EventUser>>()
    private val amountTriesCurrentQuestionPerUser = mutableMapOf<EventUser, /* amount tries: */ Int>()

    fun popRandomQuestion(): Question {
        return if(isLastTwoQuestions()) {
            questions.filter{
                it.isLast2Questions
            }
        } else {
            questions.filter{
                !it.isLast2Questions
            }
        }.filter {
            it !in askedQuestions && !it.isTieBreakerQuestion
        }.random().also {
            askedQuestions[it] = listOf()
            currentQuestion.value = it
        }
    }

    fun popRandomTieBreakerQuestion(): Question {
        if(questions.none { it !in askedQuestions && it.isTieBreakerQuestion }) {
            askedQuestions.clear()
        }

        return questions.filter {
            it.isTieBreakerQuestion && it !in askedQuestions
        }.random().also {
            askedQuestions[it] = listOf()
            currentQuestion.value = it
        }
    }

    fun updateCurrentQuestionsLeaderboard(user: EventUser) {
        val newLeaderboard = (askedQuestions[currentQuestion.value] ?: listOf()).toMutableList()

        if(newLeaderboard.size == 3 || user in newLeaderboard) {
            return
        }

        newLeaderboard += user
        logger.info("Leaderboard got updated. New leaderboard: ${newLeaderboard.joinToString(" | ")}")
        askedQuestions[currentQuestion.value] = newLeaderboard
    }

    fun getCurrentLeaderboard(): List<EventUser> {
        return askedQuestions[currentQuestion.value] ?: listOf()
    }

    fun checkAnswer(answer: String, user: EventUser): Boolean {
        return if(amountTriesCurrentQuestionPerUser[user] == maxAmountTries){
            logger.info("User ${user.name} has exceeded their tries")
            false
        } else {
            if(amountTriesCurrentQuestionPerUser[user] == null) {
                amountTriesCurrentQuestionPerUser[user] = 1
            } else {
                amountTriesCurrentQuestionPerUser[user] = amountTriesCurrentQuestionPerUser[user]!! + 1
            }
            (answer.trim() == currentQuestion.value.answer).also {
                logger.info("User ${user.name} answered the question, solution was: $it")
            }
        }
    }

    fun resetCurrentQuestion() {
        logger.info("Resetting question")
        currentQuestion.value = emptyQuestion
        amountTriesCurrentQuestionPerUser.clear()
    }

    private fun isLastTwoQuestions(): Boolean {
        return askedQuestions.size >= TwitchBotConfig.amountQuestions - 2
    }
}