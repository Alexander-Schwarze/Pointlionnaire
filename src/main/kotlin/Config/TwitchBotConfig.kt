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
    val userCooldown = properties.getProperty("user_cooldown_seconds").toInt().seconds
    val leaveEmote: String = properties.getProperty("leave_emote")
    val arriveEmote: String = properties.getProperty("arrive_emote")
    val confirmEmote: String = properties.getProperty("confirm_emote")
    val rejectEmote: String = properties.getProperty("reject_emote")
    val explanationEmote: String = properties.getProperty("explanation_emote")
}