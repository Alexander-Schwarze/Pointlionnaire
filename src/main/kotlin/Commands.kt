import com.github.twitch4j.chat.TwitchChat
import com.github.twitch4j.common.events.domain.EventUser
import commands.answerCommand
import commands.helpCommand
import commands.questionCommand
import commands.redeemCommand
import kotlin.time.Duration

data class Command(
    val names: List<String>,
    val handler: suspend CommandHandlerScope.(arguments: List<String>) -> Unit
)

data class CommandHandlerScope(
    val chat: TwitchChat,
    val user: EventUser,
    var addedUserCooldown: Duration = Duration.ZERO
)

val commands = listOf(
    helpCommand,
    questionCommand,
    answerCommand,
    redeemCommand
)