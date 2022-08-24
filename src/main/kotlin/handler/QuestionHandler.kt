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
        val instance by lazy {
            val questionsFile = File("data/questions.json")

            val questions = if (!questionsFile.exists()) {
                questionsFile.createNewFile()
                logger.info("Questions file created.")
                setOf()
            } else {
                try {
                    json.decodeFromString<Set<Question>>(questionsFile.readText())
                        .map { it.copy(answer = it.answer.lowercase(Locale.getDefault())) }
                        .toSet()
                        .also { currentQuestionsData ->
                            logger.info("Existing questions file found! Values: ${currentQuestionsData.joinToString(" | ")}")
                        }
                } catch (e: Exception) {
                    logger.warn("Error while reading questions file. Initializing empty questions", e)
                    setOf()
                }
            }

            if (
                questions.count { !it.isTieBreakerQuestion } < TwitchBotConfig.amountQuestions ||
                questions.count { !it.isLast2Questions && !it.isTieBreakerQuestion } < TwitchBotConfig.amountQuestions - 2 ||
                questions.count { it.isLast2Questions && !it.isTieBreakerQuestion } < 2 ||
                questions.none { it.isTieBreakerQuestion }
            ) {
                // TODO: Remove this as soon as questions can be added via UI
                logger.error("There was an error with given questions. As for version 1.0.0, you cannot set questions in the UI. Thus they need to be added before app start in the json-file.")
                logger.info(
                    """
                        You need:
                        - At least ${TwitchBotConfig.amountQuestions} total questions who are no tiebreaker questions.
                        - At least ${TwitchBotConfig.amountQuestions - 2} questions which are neither tiebreaker nor last 2 questions.
                        - At least 2 questions which are last 2 questions and no tiebreaker.
                        - At least 1 tiebreaker question.
                    """.trimIndent()
                )

                throw ExceptionInInitializerError()
            }

            QuestionHandler(questions)
        }
    }

    val currentQuestion: MutableStateFlow<Question?> = MutableStateFlow(null)
    val currentLeaderboard: MutableStateFlow<List<EventUser>> = MutableStateFlow(listOf())

    var askedQuestions = setOf<Question>()
    private val amountTriesCurrentQuestionPerUser = mutableMapOf<EventUser, /* amount tries: */ Int>()

    fun nextQuestion() {
        questions
            .filter { it.isLast2Questions == isLastTwoQuestions && it !in askedQuestions && !it.isTieBreakerQuestion }
            .random()
            .let {
                @Suppress("SuspiciousCollectionReassignment")
                askedQuestions += it
                currentQuestion.value = it
            }
    }

    fun nextTieBreakerQuestion() {
        if (questions.none { it !in askedQuestions && it.isTieBreakerQuestion }) {
            askedQuestions = setOf()
        }

        questions
            .filter { it.isTieBreakerQuestion && it !in askedQuestions }
            .random()
            .also {
                @Suppress("SuspiciousCollectionReassignment")
                askedQuestions += it
                currentQuestion.value = it
            }
    }

    fun updateCurrentQuestionsLeaderboard(user: EventUser) {
        if (currentLeaderboard.value.size == 3 || user in currentLeaderboard.value) {
            return
        }

        currentLeaderboard.value += user
        logger.info("Leaderboard got updated. New leaderboard: ${currentLeaderboard.value.joinToString(" | ")}")
    }

    fun checkAnswer(answer: String, user: EventUser) = if (amountTriesCurrentQuestionPerUser[user] == maxAmountTries) {
        logger.info("User ${user.name} has exceeded their tries")
        false
    } else {
        if (amountTriesCurrentQuestionPerUser[user] == null) {
            amountTriesCurrentQuestionPerUser[user] = 1
        } else {
            amountTriesCurrentQuestionPerUser[user] = amountTriesCurrentQuestionPerUser[user]!! + 1
        }

        (answer.trim() == currentQuestion.value?.answer).also {
            logger.info("User ${user.name} answered the question, solution was: $it")
        }
    }

    fun resetCurrentQuestion() {
        logger.info("Resetting current question and amount tries")
        currentQuestion.value = null
        currentLeaderboard.value = listOf()
        amountTriesCurrentQuestionPerUser.clear()
    }

    fun resetQuestions() {
        logger.info("Resetting all previous questions data")
        resetCurrentQuestion()
        askedQuestions = setOf()
        currentLeaderboard.value = listOf()
    }

    private val isLastTwoQuestions get() = askedQuestions.size >= TwitchBotConfig.amountQuestions - 2
}