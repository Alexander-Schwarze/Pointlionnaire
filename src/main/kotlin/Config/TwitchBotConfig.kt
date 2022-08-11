import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.seconds

object TwitchBotConfig {
    private val properties = Properties().apply {
        load(File("data/twitchBotConfig.properties").inputStream())
    }

    val channel: String = properties.getProperty("channel")
    val onlyMods = properties.getProperty("only_mods") == "true"
    val commandPrefix: String = properties.getProperty("command_prefix")
    val leaveEmote: String = properties.getProperty("leave_emote")
    val arriveEmote: String = properties.getProperty("arrive_emote")
    val explanationEmote: String = properties.getProperty("explanation_emote")
    val noQuestionPendingText: String = properties.getProperty("no_question_pending_text")
    val maximumRolls = properties.getProperty("maximum_rolls").toInt()
    val noMoreRerollsText: String = properties.getProperty("no_more_rerolls_text")
    val ggEmote: String = properties.getProperty("gg_emote")
}