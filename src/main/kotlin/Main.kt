import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.TwitchClient
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.github.twitch4j.common.enums.CommandPermission
import commands.helpCommand
import commands.redeemCommand
import handler.QuestionHandler
import handler.UserHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatterBuilder
import javax.swing.JOptionPane
import kotlin.system.exitProcess
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


val logger: Logger = LoggerFactory.getLogger("Bot")
val backgroundCoroutineScope = CoroutineScope(Dispatchers.IO)

val json = Json {
    prettyPrint = true
}

var intervalRunning = mutableStateOf(false)
var timestamptUntilNextAction: MutableState<Instant> = mutableStateOf(Instant.now())

suspend fun main() = try {
    setupLogging()
    val twitchClient = setupTwitchBot()
    if(!intervalHandler(twitchClient)){
        JOptionPane.showMessageDialog(null, "Error with starting the interval. Check the log for more infos!", "InfoBox: File Debugger", JOptionPane.INFORMATION_MESSAGE)
        logger.error("Error with starting the interval. Check the log for more infos!")
        exitProcess(0)
    }

    application {
        DisposableEffect(Unit) {
            onDispose {
                twitchClient.chat.sendMessage(TwitchBotConfig.channel, "Bot shutting down ${TwitchBotConfig.leaveEmote}")
                logger.info("App shutting down...")
            }
        }

        Window(
            state = WindowState(size = DpSize(500.dp, 250.dp)),
            title = "Pointlionnaire",
            onCloseRequest = ::exitApplication,
            icon = painterResource("icon.ico"),
            resizable = false
        ) {
            App()
        }
    }
} catch (e: Throwable) {
    JOptionPane.showMessageDialog(null, e.message + "\n" + StringWriter().also { e.printStackTrace(PrintWriter(it)) }, "InfoBox: File Debugger", JOptionPane.INFORMATION_MESSAGE)
    logger.error("Error while executing program.", e)
    exitProcess(0)
}

private const val LOG_DIRECTORY = "logs"

fun setupLogging() {
    Files.createDirectories(Paths.get(LOG_DIRECTORY))

    val logFileName = DateTimeFormatterBuilder()
        .appendInstant(0)
        .toFormatter()
        .format(Instant.now())
        .replace(':', '-')

    val logFile = Paths.get(LOG_DIRECTORY, "${logFileName}.log").toFile().also {
        if (!it.exists()) {
            it.createNewFile()
        }
    }

    System.setOut(PrintStream(MultiOutputStream(System.out, FileOutputStream(logFile))))

    logger.info("Log file '${logFile.name}' has been created.")
}


private suspend fun setupTwitchBot(): TwitchClient {
    val chatAccountToken = File("data/twitchtoken.txt").readText()

    val twitchClient = TwitchClientBuilder.builder()
        .withEnableHelix(true)
        .withEnableChat(true)
        .withChatAccount(OAuth2Credential("twitch", chatAccountToken))
        .build()

    val nextAllowedCommandUsageInstantPerUser = mutableMapOf<Pair<Command, /* user: */ String>, Instant>()

    twitchClient.chat.run {
        connect()
        joinChannel(TwitchBotConfig.channel)
        sendMessage(TwitchBotConfig.channel, "Bot running ${TwitchBotConfig.arriveEmote}")
    }

    twitchClient.eventManager.onEvent(ChannelMessageEvent::class.java) { messageEvent ->
        val message = messageEvent.message
        if (!message.startsWith(TwitchBotConfig.commandPrefix)) {
            return@onEvent
        }

        val parts = message.substringAfter(TwitchBotConfig.commandPrefix).split(" ")
        val command = commands.find { parts.first().lowercase() in it.names } ?: return@onEvent

        if (TwitchBotConfig.onlyMods && CommandPermission.MODERATOR !in messageEvent.permissions) {
            twitchClient.chat.sendMessage(
                TwitchBotConfig.channel,
                "You do not have the required permissions to use this command."
            )
            logger.info("User '${messageEvent.user.name}' does not have the necessary permissions to call command '${command.names.first()}'")

            return@onEvent
        }

        logger.info("User '${messageEvent.user.name}' tried using command '${command.names.first()}' with arguments: ${parts.drop(1).joinToString()}")

        val nextAllowedCommandUsageInstant = nextAllowedCommandUsageInstantPerUser.getOrPut(command to messageEvent.user.name) {
            Instant.now()
        }

        if (Instant.now().isBefore(nextAllowedCommandUsageInstant) && CommandPermission.MODERATOR !in messageEvent.permissions) {
            val secondsUntilTimeoutOver = Duration.between(Instant.now(), nextAllowedCommandUsageInstant).seconds

            twitchClient.chat.sendMessage(
                TwitchBotConfig.channel,
                "${messageEvent.user.name}, You are still on cooldown. Please try again in $secondsUntilTimeoutOver seconds."
            )
            logger.info("Unable to execute command due to ongoing user cooldown.")

            return@onEvent
        }

        val commandHandlerScope = CommandHandlerScope(
            chat = twitchClient.chat,
            user = messageEvent.user
        )

        backgroundCoroutineScope.launch {
            command.handler(commandHandlerScope, parts.drop(1))

            val key = command to messageEvent.user.name
            nextAllowedCommandUsageInstantPerUser[key] = nextAllowedCommandUsageInstantPerUser[key]!!.plus(commandHandlerScope.addedUserCooldown.toJavaDuration())
        }
    }

    logger.info("Twitch client started.")
    return twitchClient
}

fun startOrStopInterval(){
    intervalRunning.value = !intervalRunning.value
    logger.info("intervalRunning: ${intervalRunning.value}")
}
fun intervalHandler(twitchClient: TwitchClient): Boolean {
    val chat = twitchClient.chat
    val questionHandlerInstance = QuestionHandler.instance ?: run {
        logger.error("question handler instance is null. Aborting...")
        return false
    }
    val delayBeforeQuestion = 10.seconds
    val durationUntilNextQuestion = TwitchBotConfig.totalIntervalDuration / TwitchBotConfig.amountQuestions - TwitchBotConfig.answerDuration - delayBeforeQuestion
    if(durationUntilNextQuestion <= TwitchBotConfig.answerDuration){
        logger.error("durationUntilNextQuestion is smaller than answer duration. Aborting...")
        return false
    }
    val points = try {
        mapOf(0 to TwitchBotConfig.pointsForTop3[0], 1 to TwitchBotConfig.pointsForTop3[1], 2 to TwitchBotConfig.pointsForTop3[2])
    } catch (e: Exception){
        return false
    }

    backgroundCoroutineScope.launch {
        while (true){
            // TODO: Comment in the delays
            if(intervalRunning.value){
                logger.info("Interval running. Amount of asked questions: ${questionHandlerInstance.askedQuestions.size}")
                if(questionHandlerInstance.askedQuestions.isEmpty()){
                    // Starting the interval
                    chat.sendMessage(TwitchBotConfig.channel,
                        "${TwitchBotConfig.attentionEmote} Attention, Attention ${TwitchBotConfig.attentionEmote} The Questions are about to begin! " +
                                "You wanna know, how to participate? ${TwitchBotConfig.amountQuestions} Questions will be asked during the next ${TwitchBotConfig.totalIntervalDuration} and you have per question ${TwitchBotConfig.answerDuration} to answer! ${TwitchBotConfig.explanationEmote}"
                    )

                    //delay(10.seconds)

                    chat.sendMessage(TwitchBotConfig.channel,
                        "You will have ${QuestionHandler.instance.maxAmountTries} tries to get the answer right. Wanna know, how to answer? Type \"${TwitchBotConfig.commandPrefix}${helpCommand.names.first()}\" to see all commands!"
                    )

                    //delay(10.seconds)

                    chat.sendMessage(TwitchBotConfig.channel,
                        "The winner will be announced at the end. They can get a random price by typing \"${TwitchBotConfig.commandPrefix}${redeemCommand.names.first()}\". How cool is that?! ${TwitchBotConfig.ggEmote}"
                    )

                    //delay(30.seconds)
                }

                chat.sendMessage(TwitchBotConfig.channel, "Tighten your seatbelts, the question is coming up!")
                //delay(delayBeforeQuestion)
                chat.sendMessage(TwitchBotConfig.channel, questionHandlerInstance.popRandomQuestion().also { logger.info("Current question: ${it.questionText} | Current answer: ${it.answer}") }.questionText)

                delay(TwitchBotConfig.answerDuration)
                chat.sendMessage(TwitchBotConfig.channel, "The time is up! ${TwitchBotConfig.timeUpEmote}")
                logger.info("Answer duration is over")

                logger.info("Updating leaderboard...")
                var i = 0
                questionHandlerInstance.getCurrentLeaderboard().forEach {
                    points[i]?.let { it1 ->
                        UserHandler.updateLeaderBoard(it,
                            if(questionHandlerInstance.isLastTwoQuestions()){
                                it1 * 2
                            } else {
                                it1
                            }
                        )
                    }
                    i++
                }

                logger.info("Resetting question")
                questionHandlerInstance.resetCurrentQuestion()

                if(questionHandlerInstance.askedQuestions.size == TwitchBotConfig.amountQuestions){
                    break
                }

                chat.sendMessage(TwitchBotConfig.channel, "Next question will be in $durationUntilNextQuestion")
                delay(durationUntilNextQuestion)
            }
        }

        if(UserHandler.getTieBreakerUsers().size > 1) {

            chat.sendMessage(
                TwitchBotConfig.channel,
                "Oh oh! Looks like we have a tie ${TwitchBotConfig.tieEmote}"
            )

            // TODO: Tie breaker handling
        }

        chat.sendMessage(
            TwitchBotConfig.channel,
            "The game is over! ${TwitchBotConfig.gameUpEmote}"
        )
        UserHandler.setWinner()
        delay(5.seconds)
        if(UserHandler.winner != null) {
            chat.sendMessage(
                TwitchBotConfig.channel,
                "The results are in and the winner is: ${UserHandler.winner?.userName} ${TwitchBotConfig.ggEmote}"
            )
            delay(4.seconds)

            val leaderBoard = UserHandler.getTop3Users()
            chat.sendMessage(
                TwitchBotConfig.channel,
                "The Top 3 leaderboard: First: ${leaderBoard[0].userName}, Second: ${leaderBoard[1].userName}, Third: ${leaderBoard[2].userName}"
            )

            delay(5.seconds)

            chat.sendMessage(
                TwitchBotConfig.channel,
                "${UserHandler.winner!!.userName}, you now have the opportunity to redeem a random prize by using ${TwitchBotConfig.commandPrefix}${redeemCommand.names.first()}. You can do this until I get shut down!"
            )
        } else {
            chat.sendMessage(
                TwitchBotConfig.channel,
                "Imagine getting a single answer right... couldn't be you guys ${TwitchBotConfig.noWinnerEmote}"
            )
        }

    }

    return true
}