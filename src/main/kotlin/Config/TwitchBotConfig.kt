import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.minutes

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
    val amountQuestions = properties.getProperty("amount_questions").toInt()
    val totalIntervalDuration = properties.getProperty("total_interval_duration").toDouble().minutes
    val answerDuration = properties.getProperty("answer_duration").toDouble().minutes
    val attentionEmote: String = properties.getProperty("attention_emote")
    val timeUpEmote: String = properties.getProperty("time_up_emote")
    val pointsForTop3: List<Int> = properties.getProperty("points_for_top_3").split(",").map{ it.toInt() }
    val gameUpEmote: String = properties.getProperty("game_up_emote")
    val noWinnerEmote: String = properties.getProperty("no_winner_emote")
    val tieEmote: String = properties.getProperty("tie_emote")
    val tiebreakerAnswerDuration = properties.getProperty("tiebreaker_answer_duration").toDouble().minutes
}