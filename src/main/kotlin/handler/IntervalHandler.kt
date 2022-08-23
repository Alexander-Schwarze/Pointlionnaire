package handler

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import backgroundCoroutineScope
import com.github.twitch4j.chat.TwitchChat
import commands.helpCommand
import commands.redeemCommand
import intervalRunning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import logger
import kotlin.time.Duration.Companion.seconds

// TODO: Maybe this should be a class instead?
object IntervalHandler {

    fun startOrStopInterval() {
        intervalRunning.value = !intervalRunning.value
        logger.info("intervalRunning: ${intervalRunning.value}")
    }

    var timestampNextAction: MutableState<Instant?> = mutableStateOf(null)

    // TODO: This needs to be an Object (I think) with properties and an interval handling function
    fun intervalHandler(chat: TwitchChat): Boolean {
        val questionHandlerInstance = QuestionHandler.instance ?: run {
            logger.error("questionHandlerInstance is null. Aborting...")
            return false
        }
        val delayBeforeQuestion = 10.seconds
        val durationUntilNextQuestion = TwitchBotConfig.totalIntervalDuration / TwitchBotConfig.amountQuestions - TwitchBotConfig.answerDuration - delayBeforeQuestion
        if(durationUntilNextQuestion <= TwitchBotConfig.answerDuration){
            logger.error("durationUntilNextQuestion is smaller than answer duration. Aborting...")
            return false
        }

        val durationUntilNextTieQuestion = TwitchBotConfig.tiebreakerAnswerDuration * 2
        val points = try {
            mapOf(0 to TwitchBotConfig.pointsForTop3[0], 1 to TwitchBotConfig.pointsForTop3[1], 2 to TwitchBotConfig.pointsForTop3[2])
        } catch (e: Exception){
            logger.error("Error while accessing points List. Aborting...")
            return false
        }
        val explanationDelays = listOf(10.seconds, 10.seconds, 30.seconds)

        backgroundCoroutineScope.launch {
            while (true){
                if(intervalRunning.value){
                    timestampNextAction.value = Clock.System.now() + explanationDelays.sumOf { it.inWholeSeconds }.seconds + delayBeforeQuestion
                    logger.info("Interval running. Amount of asked questions: ${questionHandlerInstance.askedQuestions.size}")
                    if(questionHandlerInstance.askedQuestions.isEmpty()){

                        logger.info("Interval is starting. Sending the info messages.")
                        chat.sendMessage(
                            TwitchBotConfig.channel,
                            "${TwitchBotConfig.attentionEmote} Attention, Attention ${TwitchBotConfig.attentionEmote} The Quiz show is about to begin! " +
                                    "You wanna know how to participate? ${TwitchBotConfig.amountQuestions} " +
                                    "Question".run {
                                        if(TwitchBotConfig.amountQuestions > 1){
                                            this + "s"
                                        } else {
                                            this
                                        }
                                    } +
                                    " will be asked during the next ${TwitchBotConfig.totalIntervalDuration} and you have per question ${TwitchBotConfig.answerDuration} to answer! ${TwitchBotConfig.explanationEmote}"
                        )

                        delay(explanationDelays[0])

                        chat.sendMessage(
                            TwitchBotConfig.channel,
                            "You will have ${TwitchBotConfig.maxAmountTries} " +
                                    "tr".run {
                                        if(TwitchBotConfig.maxAmountTries > 1){
                                            this + "ies"
                                        } else {
                                            this + "y"
                                        }
                                    } +
                                    " to get the answer right. Wanna know, how to answer? Type \"${TwitchBotConfig.commandPrefix}${helpCommand.names.first()}\" to see all commands!"
                        )

                        delay(explanationDelays[1])

                        chat.sendMessage(
                            TwitchBotConfig.channel,
                            "The winner will be announced at the end. They can get a random prize by typing \"${TwitchBotConfig.commandPrefix}${redeemCommand.names.first()}\". How cool is that?! ${TwitchBotConfig.ggEmote}"
                        )

                        delay(explanationDelays[2])
                    }

                    timestampNextAction.value = Clock.System.now() + delayBeforeQuestion
                    chat.sendMessage(
                        TwitchBotConfig.channel,
                        "Tighten your seatbelts, the question is coming up!"
                    )
                    delay(delayBeforeQuestion)

                    val currentQuestion = questionHandlerInstance.popRandomQuestion().also {
                        logger.info("Current question: ${it.questionText} | Current answer: ${it.answer}")
                    }
                    chat.sendMessage(
                        TwitchBotConfig.channel,
                        "——————————————————————" +
                                "Question ${questionHandlerInstance.askedQuestions.size}: " + currentQuestion.questionText +
                                "——————————————————————"
                    )

                    timestampNextAction.value = Clock.System.now() + TwitchBotConfig.answerDuration
                    delay(TwitchBotConfig.answerDuration)
                    chat.sendMessage(
                        TwitchBotConfig.channel,
                        "The time is up! ${TwitchBotConfig.timeUpEmote}"
                    )
                    logger.info("Answer duration is over")

                    logger.info("Updating leaderboard")
                    questionHandlerInstance.getCurrentLeaderboard().forEachIndexed { index, user ->
                        points[index]?.let { currentPoints ->
                            UserHandler.updateLeaderBoard(user, currentPoints.run {
                                if(currentQuestion.isLast2Questions){
                                    this * 2
                                } else {
                                    this
                                }
                            })
                        }
                    }

                    questionHandlerInstance.resetCurrentQuestion()

                    if(questionHandlerInstance.askedQuestions.size == TwitchBotConfig.amountQuestions){
                        break
                    }

                    chat.sendMessage(
                        TwitchBotConfig.channel,
                        "Next question will be in ${durationUntilNextQuestion + delayBeforeQuestion}"
                    )

                    timestampNextAction.value = Clock.System.now() + durationUntilNextQuestion + delayBeforeQuestion
                    delay(durationUntilNextQuestion)
                }
            }

            if(UserHandler.getTieBreakerUser().size > 1) {
                logger.info("First users are tied. Starting tie breaker handling")
                UserHandler.setTieBreakerUser()
                chat.sendMessage(
                    TwitchBotConfig.channel,
                    "Oh oh! Looks like we have a tie ${TwitchBotConfig.tieEmote}"
                )

                timestampNextAction.value = Clock.System.now() + 10.seconds * 2 + delayBeforeQuestion
                delay(10.seconds)
                logger.info("Tie breaker users: ${UserHandler.tieBreakUsers.joinToString(" | ")}")
                chat.sendMessage(
                    TwitchBotConfig.channel,
                    "The users ${UserHandler.tieBreakUsers.map { it.name }.let { users ->
                        listOf(users.dropLast(1).joinToString(), users.last()).filter { it.isNotBlank() }.joinToString(" and ")}
                    } are tied first place and will have to answer tie breaker questions. Whoever is the quickest to answer correctly first wins!"
                )
                delay(10.seconds)

                while (true){
                    chat.sendMessage(TwitchBotConfig.channel, "${UserHandler.tieBreakUsers.joinToString{ it.name }} - Get Ready! The question is coming up!")
                    timestampNextAction.value = Clock.System.now() + delayBeforeQuestion
                    delay(delayBeforeQuestion)

                    chat.sendMessage(
                        TwitchBotConfig.channel,
                        "——————————————————————" +
                                questionHandlerInstance.popRandomTieBreakerQuestion().also {
                                    logger.info("Current question: ${it.questionText} | Current answer: ${it.answer}")
                                }.questionText +
                                "——————————————————————"
                    )

                    timestampNextAction.value = Clock.System.now() + TwitchBotConfig.tiebreakerAnswerDuration
                    delay(TwitchBotConfig.tiebreakerAnswerDuration)

                    chat.sendMessage(
                        TwitchBotConfig.channel,
                        "The time is up! ${TwitchBotConfig.timeUpEmote}"
                    )
                    logger.info("Answer duration is over")

                    logger.info("Updating leaderboard...")
                    questionHandlerInstance.getCurrentLeaderboard().forEachIndexed { index, user ->
                        points[index]?.let { currentPoints ->
                            UserHandler.updateLeaderBoard(user, currentPoints)
                        }
                    }

                    questionHandlerInstance.resetCurrentQuestion()

                    if(UserHandler.getTieBreakerUser().size == 1){
                        break
                    }

                    chat.sendMessage(
                        TwitchBotConfig.channel,
                        "No one got it right? Well... next question will be in ${durationUntilNextTieQuestion + delayBeforeQuestion}"
                    )
                    timestampNextAction.value = Clock.System.now() + durationUntilNextTieQuestion + delayBeforeQuestion
                    delay(durationUntilNextTieQuestion)
                }
            }

            logger.info("The Game ended. Evaluating results")

            chat.sendMessage(
                TwitchBotConfig.channel,
                "The game is over! ${TwitchBotConfig.gameUpEmote}"
            )

            UserHandler.setWinner()

            delay(5.seconds)

            if(UserHandler.winner != null) {
                chat.sendMessage(
                    TwitchBotConfig.channel,
                    "The results are in and the winner is: ${UserHandler.winner?.name} ${TwitchBotConfig.ggEmote}"
                )
                delay(4.seconds)

                val leaderBoard = UserHandler.getTop3Users().also {
                    logger.info("Leaderboard at the end: First: ${it[0]?.name}, Second: ${it[1]?.name}, Third: ${it[2]?.name}")
                }
                chat.sendMessage(
                    TwitchBotConfig.channel,
                    "The Top 3 leaderboard: First: ${leaderBoard[0]?.name ?: "No one"}, Second: ${leaderBoard[1]?.name ?: "No one"}, Third: ${leaderBoard[2]?.name ?: "No one"}"
                )

                delay(5.seconds)

                logger.info("The winner is: ${UserHandler.winner}")
                chat.sendMessage(
                    TwitchBotConfig.channel,
                    "${UserHandler.winner!!.name}, you now have the opportunity to redeem a random prize by using ${TwitchBotConfig.commandPrefix}${redeemCommand.names.first()}. You can do this until I get shut down!"
                )
            } else {
                logger.info("No one got any answer right")

                chat.sendMessage(
                    TwitchBotConfig.channel,
                    "Imagine getting a single answer right... couldn't be you guys ${TwitchBotConfig.noWinnerEmote}"
                )
            }

        }
        return true
    }
}