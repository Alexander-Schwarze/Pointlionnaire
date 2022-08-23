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
import com.github.twitch4j.chat.TwitchChat
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.github.twitch4j.common.enums.CommandPermission
import commands.helpCommand
import commands.redeemCommand
import handler.IntervalHandler
import handler.IntervalHandler.intervalHandler
import handler.QuestionHandler
import handler.RedeemHandler
import handler.UserHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatterBuilder
import javax.swing.JOptionPane
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds


val logger: Logger = LoggerFactory.getLogger("Bot")
val backgroundCoroutineScope = CoroutineScope(Dispatchers.IO)

val json = Json {
    prettyPrint = true
}

var intervalRunning = mutableStateOf(false)

suspend fun main() = try {
    setupLogging()
    val twitchClient = setupTwitchBot()
    TwitchChatHandler.chat = twitchClient.chat
    if(!intervalHandler(twitchClient.chat)){
        JOptionPane.showMessageDialog(null, "Error with starting the interval. Check the log for more infos!", "InfoBox: File Debugger", JOptionPane.INFORMATION_MESSAGE)
        logger.error("Error with starting the interval. Check the log for more infos!")
        exitProcess(0)
    }

    // TODO: This check should be somewhere else, but not in the RedeemCommand. Though the redeem command is the first place where they are used
    if(RedeemHandler.instance == null) {
        JOptionPane.showMessageDialog(null, "Error with reading the redeems. Check the log for more infos!", "InfoBox: File Debugger", JOptionPane.INFORMATION_MESSAGE)
        logger.error("Error with setting up the redeems. Check the log for more infos!")
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

        if(messageEvent.user.name in TwitchBotConfig.blacklistedUsers || messageEvent.user.id in TwitchBotConfig.blacklistedUsers){
            twitchClient.chat.sendMessage(
                TwitchBotConfig.channel,
                "Imagine not being a blacklisted user. Couldn't be you ${messageEvent.user.name} ${TwitchBotConfig.blacklistEmote}"
            )
            if(messageEvent.user.id !in TwitchBotConfig.blacklistedUsers) {
                logger.warn("Blacklisted user ${messageEvent.user.name} tried using a command. Please use following ID in the properties file instead of the name: ${messageEvent.user.id}")
            }
            return@onEvent
        }

        logger.info("User '${messageEvent.user.name}' tried using command '${command.names.first()}' with arguments: ${parts.drop(1).joinToString()}")

        val nextAllowedCommandUsageInstant = nextAllowedCommandUsageInstantPerUser.getOrPut(command to messageEvent.user.name) {
            Clock.System.now()
        }

        if ((Clock.System.now() - nextAllowedCommandUsageInstant).isNegative() && CommandPermission.MODERATOR !in messageEvent.permissions) {
            val secondsUntilTimeoutOver = nextAllowedCommandUsageInstant - Clock.System.now()

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
            nextAllowedCommandUsageInstantPerUser[key] = nextAllowedCommandUsageInstantPerUser[key]!! + commandHandlerScope.addedUserCooldown
        }
    }

    logger.info("Twitch client started.")
    return twitchClient
}

private const val LOG_DIRECTORY = "logs"

fun setupLogging() {
    Files.createDirectories(Paths.get(LOG_DIRECTORY))

    val logFileName = DateTimeFormatterBuilder()
        .appendInstant(0)
        .toFormatter()
        .format(Clock.System.now().toJavaInstant())
        .replace(':', '-')

    val logFile = Paths.get(LOG_DIRECTORY, "${logFileName}.log").toFile().also {
        if (!it.exists()) {
            it.createNewFile()
        }
    }

    System.setOut(PrintStream(MultiOutputStream(System.out, FileOutputStream(logFile))))

    logger.info("Log file '${logFile.name}' has been created.")
}