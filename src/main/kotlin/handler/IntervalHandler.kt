package handler

import TwitchBotConfig
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import backgroundCoroutineScope
import commands.helpCommand
import commands.redeemCommand
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import logger
import pluralForm
import twitchChat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class IntervalHandler private constructor(
    private val durationUntilNextQuestion: Duration,
    private val points: Map<Int, Int>
) {
    companion object {
        val instance by lazy {
            val durationUntilNextQuestion =
                TwitchBotConfig.totalIntervalDuration / TwitchBotConfig.amountQuestions - TwitchBotConfig.answerDuration - delayBeforeQuestion

            if (durationUntilNextQuestion <= TwitchBotConfig.answerDuration) {
                logger.error("durationUntilNextQuestion is smaller than answer duration. Aborting...")
                throw ExceptionInInitializerError()
            }

            val points = try {
                mapOf(
                    0 to TwitchBotConfig.pointsForTop3[0],
                    1 to TwitchBotConfig.pointsForTop3[1],
                    2 to TwitchBotConfig.pointsForTop3[2]
                )
            } catch (e: Exception) {
                logger.error("Error while accessing points List. Aborting...")
                throw ExceptionInInitializerError()
            }

            IntervalHandler(durationUntilNextQuestion, points)
        }

        val delayBeforeQuestion = 10.seconds
    }

    private val questionHandlerInstance = QuestionHandler.instance
    val intervalRunning = mutableStateOf(false)

    var timestampNextAction: MutableState<Instant?> = mutableStateOf(null)

    private val durationUntilNextTieQuestion = TwitchBotConfig.tiebreakerAnswerDuration * 2
    private val explanationDelays = listOf(10.seconds, 10.seconds, 30.seconds)
    private var currentInterval: Job? = null

    fun stopInterval() {
        if (currentInterval != null) {
            logger.info("Current interval getting stopped by force")
            currentInterval!!.cancel()
            timestampNextAction.value = null
            intervalRunning.value = false
            QuestionHandler.instance.resetQuestions()
            RedeemHandler.instance.resetRedeems()
            UserHandler.resetUsers()
            twitchChat.sendMessage(
                TwitchBotConfig.channel,
                "Interval was force stopped, what happened? ${TwitchBotConfig.somethingWentWrongEmote}"
            )
        } else {
            logger.error("currentInterval is null and cannot be stopped. Something went wrong")
        }
    }

    fun startInterval() {
        intervalRunning.value = true

        currentInterval = backgroundCoroutineScope.launch {
            while (true) {
                logger.info("Interval running. Amount of asked questions: ${questionHandlerInstance.askedQuestions.size}")
                if (questionHandlerInstance.askedQuestions.isEmpty()) {
                    timestampNextAction.value =
                        Clock.System.now() + explanationDelays.sumOf { it.inWholeSeconds }.seconds + delayBeforeQuestion

                    logger.info("Interval is starting. Sending the info messages.")
                    twitchChat.sendMessage(
                        TwitchBotConfig.channel,
                        """
                            ${TwitchBotConfig.attentionEmote} Attention, Attention ${TwitchBotConfig.attentionEmote} The Quiz show is about to begin!
                            You wanna know how to participate? ${TwitchBotConfig.amountQuestions}
                            ${"question".pluralForm(TwitchBotConfig.amountQuestions)} will be asked during the next ${TwitchBotConfig.totalIntervalDuration} and 
                            you have ${TwitchBotConfig.answerDuration} per question to answer! ${TwitchBotConfig.explanationEmote}"
                        """.trimIndent()
                    )

                    delay(explanationDelays[0])

                    twitchChat.sendMessage(
                        TwitchBotConfig.channel,
                        """
                            You will have ${TwitchBotConfig.maxAmountTries} ${"try".pluralForm(TwitchBotConfig.maxAmountTries)} to get the answer right. 
                            Wanna know, how to answer? Type "${TwitchBotConfig.commandPrefix}${helpCommand.names.first()}" to see all commands!
                        """.trimIndent()
                    )

                    delay(explanationDelays[1])

                    twitchChat.sendMessage(
                        TwitchBotConfig.channel,
                        """
                            The winner will be announced at the end. They can get a random prize by typing "${TwitchBotConfig.commandPrefix}${redeemCommand.names.first()}". 
                            How cool is that?! ${TwitchBotConfig.ggEmote}
                        """
                    )

                    delay(explanationDelays[2])
                }

                timestampNextAction.value = Clock.System.now() + delayBeforeQuestion
                twitchChat.sendMessage(
                    TwitchBotConfig.channel,
                    "Tighten your seatbelts, the question is coming up!"
                )
                delay(delayBeforeQuestion)

                questionHandlerInstance.nextQuestion()
                val currentQuestion = questionHandlerInstance.currentQuestion.value.also {
                    if (it != null) {
                        logger.info("Current question: ${it.questionText} | Current answer: ${it.answer}")
                    }
                }

                if (currentQuestion != null) {
                    twitchChat.sendMessage(
                        TwitchBotConfig.channel,
                        """
                            ——————————————————————
                            Question ${questionHandlerInstance.askedQuestions.size}: ${currentQuestion.questionText}
                            ——————————————————————
                        """.trimIndent()
                    )
                }

                timestampNextAction.value = Clock.System.now() + TwitchBotConfig.answerDuration
                delay(TwitchBotConfig.answerDuration)
                twitchChat.sendMessage(
                    TwitchBotConfig.channel,
                    "The time is up! ${TwitchBotConfig.timeUpEmote}"
                )
                logger.info("Answer duration is over")

                logger.info("Updating leaderboard")
                questionHandlerInstance.currentLeaderboard.value.forEachIndexed { index, user ->
                    points[index]?.let { currentPoints ->
                        if (currentQuestion != null) {
                            UserHandler.updateLeaderBoard(user, currentPoints.run {
                                if (currentQuestion.isLast2Questions) {
                                    this * 2
                                } else {
                                    this
                                }
                            })
                        }
                    }
                }

                questionHandlerInstance.resetCurrentQuestion()

                if (questionHandlerInstance.askedQuestions.size == TwitchBotConfig.amountQuestions) {
                    break
                }

                twitchChat.sendMessage(
                    TwitchBotConfig.channel,
                    "Next question will be in ${durationUntilNextQuestion + delayBeforeQuestion}"
                )

                timestampNextAction.value = Clock.System.now() + durationUntilNextQuestion + delayBeforeQuestion
                delay(durationUntilNextQuestion)

            }

            if (UserHandler.getTieBreakerUser().size > 1) {
                logger.info("First users are tied. Starting tie breaker handling")
                twitchChat.sendMessage(
                    TwitchBotConfig.channel,
                    "Oh oh! Looks like we have a tie ${TwitchBotConfig.tieEmote}"
                )

                timestampNextAction.value = Clock.System.now() + 10.seconds * 2 + delayBeforeQuestion
                delay(10.seconds)
                logger.info("Tie breaker users: ${UserHandler.getTieBreakerUser().joinToString(" | ")}")
                twitchChat.sendMessage(
                    TwitchBotConfig.channel,
                    "The users ${
                        UserHandler.getTieBreakerUser().map { it.name }.let { users ->
                            listOf(users.dropLast(1).joinToString(), users.last()).filter { it.isNotBlank() }
                                .joinToString(" and ")
                        }
                    } are tied first place and will have to answer tie breaker questions. Whoever is the quickest to answer correctly first wins!"
                )
                delay(10.seconds)

                while (true) {
                    twitchChat.sendMessage(
                        TwitchBotConfig.channel,
                        "${UserHandler.getTieBreakerUser().joinToString { it.name }} - Get Ready! The question is coming up!"
                    )
                    timestampNextAction.value = Clock.System.now() + delayBeforeQuestion
                    delay(delayBeforeQuestion)

                    questionHandlerInstance.nextTieBreakerQuestion()
                    twitchChat.sendMessage(
                        TwitchBotConfig.channel,
                        """
                            ——————————————————————
                            ${
                                questionHandlerInstance.currentQuestion.value.also {
                                    if (it != null) {
                                        logger.info("Current question: ${it.questionText} | Current answer: ${it.answer}")
                                    }
                                }?.questionText
                            }
                            ——————————————————————
                        """.trimIndent()
                    )

                    timestampNextAction.value = Clock.System.now() + TwitchBotConfig.tiebreakerAnswerDuration
                    delay(TwitchBotConfig.tiebreakerAnswerDuration)

                    twitchChat.sendMessage(
                        TwitchBotConfig.channel,
                        "The time is up! ${TwitchBotConfig.timeUpEmote}"
                    )
                    logger.info("Answer duration is over")

                    logger.info("Updating leaderboard...")
                    questionHandlerInstance.currentLeaderboard.value.forEachIndexed { index, user ->
                        points[index]?.let { currentPoints ->
                            UserHandler.updateLeaderBoard(user, currentPoints)
                        }
                    }

                    questionHandlerInstance.resetCurrentQuestion()

                    if (UserHandler.getTieBreakerUser().size == 1) {
                        break
                    }

                    twitchChat.sendMessage(
                        TwitchBotConfig.channel,
                        "No one got it right? Well... next question will be in ${durationUntilNextTieQuestion + delayBeforeQuestion}"
                    )
                    timestampNextAction.value = Clock.System.now() + durationUntilNextTieQuestion + delayBeforeQuestion
                    delay(durationUntilNextTieQuestion)
                }
            }

            logger.info("The Game ended. Evaluating results")

            twitchChat.sendMessage(
                TwitchBotConfig.channel,
                "The game is over! ${TwitchBotConfig.gameUpEmote}"
            )

            UserHandler.setWinner()

            delay(5.seconds)

            if (UserHandler.winner != null) {
                twitchChat.sendMessage(
                    TwitchBotConfig.channel,
                    "The results are in and the winner is: ${UserHandler.winner?.name} ${TwitchBotConfig.ggEmote}"
                )
                delay(4.seconds)

                val leaderBoard = UserHandler.getTop3Users().also {
                    logger.info("Leaderboard at the end: First: ${it[0].name}, Second: ${it[1].name}, Third: ${it[2].name}")
                }
                twitchChat.sendMessage(
                    TwitchBotConfig.channel,
                    "The Top 3 leaderboard: First: ${leaderBoard[0].name ?: "No one"}, Second: ${leaderBoard[1].name ?: "No one"}, Third: ${leaderBoard[2].name ?: "No one"}"
                )

                delay(5.seconds)

                logger.info("The winner is: ${UserHandler.winner}")
                twitchChat.sendMessage(
                    TwitchBotConfig.channel,
                    "${UserHandler.winner!!.name}, you now have the opportunity to redeem a random prize by using ${TwitchBotConfig.commandPrefix}${redeemCommand.names.first()}. You can do this until I get shut down!"
                )
            } else {
                logger.info("No one got any answer right")

                twitchChat.sendMessage(
                    TwitchBotConfig.channel,
                    "Imagine getting a single answer right... couldn't be you guys ${TwitchBotConfig.noWinnerEmote}"
                )
            }

            logger.info("Current interval ended.")
        }
    }
}